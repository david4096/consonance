/*
 *     Consonance - workflow software for multiple clouds
 *     Copyright (C) 2016 OICR
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package io.consonance.arch.persistence;

import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.beans.ProvisionState;
import io.consonance.common.CommonTestUtilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author dyuen
 */
public class PostgreSQLIT {
    private PostgreSQL postgres;

    @BeforeClass
    public static void setup() throws IOException, TimeoutException {
        CommonTestUtilities.clearState();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        File configFile = FileUtils.getFile("src", "test", "resources", "config");
        HierarchicalINIConfiguration parseConfig = CommonTestUtilities.parseConfig(configFile.getAbsolutePath());
        this.postgres = new PostgreSQL(parseConfig);

        // clean up the database
        postgres.clearDatabase();
    }

    @After
    public void tearDown() throws Exception {
    }

    /**
     * Test of getPendingProvisionUUID method, of class PostgreSQL.
     */
    @Test
    public void testGetPendingProvisionUUID() {
        Provision p = createProvision();
        p.setProvisionUUID("provision_uuid");
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        String result = postgres.getPendingProvisionUUID();
        assertEquals("provision_uuid", result);
    }

    /**
     * Test of updatePendingProvision method, of class PostgreSQL.
     */
    @Test
    public void testUpdatePendingProvision() {
        Provision p = createProvision();
        p.setProvisionUUID("provision_uuid");
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.RUNNING);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.updatePendingProvision("provision_uuid");
        result = postgres.getProvisionCount(ProvisionState.RUNNING);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of finishContainer method, of class PostgreSQL.
     */
    @Test
    public void testFinishContainer() {
        Provision p = createProvision();
        p.setProvisionUUID("provision_uuid");
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.finishContainer("provision_uuid");
        result = postgres.getProvisionCount(ProvisionState.SUCCESS);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of finishJob method, of class PostgreSQL.
     */
    @Test
    public void testFinishJob() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        String uuid = postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number" + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.SUCCESS);
        Assert.assertTrue("found jobs, incorrect number" + jobs2.size(), jobs2.isEmpty());
        postgres.finishJob(uuid);
        List<Job> jobs3 = postgres.getJobs(JobState.SUCCESS);
        Assert.assertTrue("found jobs, incorrect number" + jobs3.size(), jobs3.size() == 1);
    }

    /**
     * Test of updateJob method, of class PostgreSQL.
     */
    @Test
    public void testUpdateJob() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        String uuid = postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number " + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number " + jobs2.size(), jobs2.size() == 1);
        postgres.updateJob(uuid, "none", JobState.FAILED);
        List<Job> jobs3 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number " + jobs3.size(), jobs3.isEmpty());
    }

    /**
     * Test of updateProvisionByProvisionUUID method, of class PostgreSQL.
     */
    @Test
    public void testUpdateProvisionByProvisionUUID() {
        Provision p = createProvision();
        p.setJobUUID("job_uuid");
        p.setProvisionUUID("provision_uuid");
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.updateProvisionByProvisionUUID("provision_uuid", "job_uuid", ProvisionState.FAILED, "9.9.9.9");
        result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of updateProvisionByJobUUID method, of class PostgreSQL.
     */
    @Test
    public void testUpdateProvisionByJobUUID() {
        Provision p = createProvision();
        p.setJobUUID("job_uuid");
        p.setProvisionUUID("provision_uuid");
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not count provisions " + result, result == 0);
        postgres.updateProvisionByJobUUID("job_uuid", "provision_uuid", ProvisionState.FAILED, "9.9.9.9");
        result = postgres.getProvisionCount(ProvisionState.FAILED);
        Assert.assertTrue("could not update provisions " + result, result == 1);
    }

    /**
     * Test of getProvisionCount method, of class PostgreSQL.
     */
    @Test
    public void testGetProvisionCount() {
        Provision p = createProvision();
        p.setState(ProvisionState.PENDING);
        postgres.createProvision(p);
        long result = postgres.getProvisionCount(ProvisionState.PENDING);
        Assert.assertTrue("could not count provisions " + result, result == 1);
    }

    /**
     * Test of createProvision method, of class PostgreSQL.
     */
    @Test
    public void testCreateProvision() {
        Provision p = createProvision();
        Integer id = postgres.createProvision(p);
        Assert.assertTrue("could not create provision " + p.toJSON(), id != null);
    }

    public Provision createProvision() {
        int cores = 8;
        int memGb = 128;
        int storageGb = 1024;
        ArrayList<String> a = new ArrayList<>();
        a.add("ansible_playbook_path");
        return new Provision(cores, memGb, storageGb, a);
    }

    public Job createJob() {
        String uuid = UUID.randomUUID().toString().toLowerCase();
        Job job = new Job(uuid);
        job.setFlavour("m1.xlarge");
        job.setEndUser("player");
        job.setContainerImageDescriptor(RandomStringUtils.randomAscii(50));
        job.setContainerRuntimeDescriptor(RandomStringUtils.randomAscii(50));
        Job.ExtraFile file = new Job.ExtraFile(RandomStringUtils.randomAscii(50), false);
        job.getExtraFiles().put("/tmp/test", file);
        job.setStderr(RandomStringUtils.randomAscii(50));
        job.setStdout(RandomStringUtils.randomAscii(50));
        return job;
    }

    /**
     * Test of createJob method, of class PostgreSQL.
     */
    @Test
    public void testCreateJob() {
        Job j = createJob();
        String result = postgres.createJob(j);
        Assert.assertTrue("could not create job " + j.toJSON(), !result.isEmpty());
    }

    /**
     * Test of getJobs method, of class PostgreSQL.
     */
    @Test
    public void testGetJobs() {
        postgres.createJob(createJob());
        postgres.createJob(createJob());
        // create one job with a defined state
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        postgres.createJob(createJob);
        // get everything
        List<Job> jobs = postgres.getJobs(null);
        Assert.assertTrue("found jobs, incorrect number" + jobs.size(), jobs.size() == 3);
        List<Job> jobs2 = postgres.getJobs(JobState.PENDING);
        Assert.assertTrue("found jobs, incorrect number" + jobs2.size(), jobs2.size() == 1);

    }

    /**
     * Test of previouslyRun method, of class PostgreSQL.
     */
    @Test
    public void testPreviouslyRun() {
        // pending should return true
        Job createJob = createJob();
        createJob.setState(JobState.PENDING);
        createJob.setJobHash("test_hash");
        postgres.createJob(createJob);
        boolean result = postgres.previouslyRun("test_hash");
        assertEquals(true, result);

        // failed should not return true
        createJob = createJob();
        createJob.setState(JobState.FAILED);
        createJob.setJobHash("test_hash2");
        postgres.createJob(createJob);
        result = postgres.previouslyRun("test_hash2");
        assertEquals(false, result);

        // success should return true
        createJob = createJob();
        createJob.setState(JobState.SUCCESS);
        createJob.setJobHash("test_hash3");
        postgres.createJob(createJob);
        result = postgres.previouslyRun("test_hash3");
        assertEquals(true, result);
    }

    /**
     * Test of clearDatabase method, of class PostgreSQL.
     */
    @Test
    public void testClearDatabase() {
        postgres.clearDatabase();
    }

    /**
     * Test of getDesiredNumberOfVMs method, of class PostgreSQL.
     */
    @Test
    public void testGetDesiredNumberOfVMs() {
        Provision p = createProvision();
        for (ProvisionState state : ProvisionState.values()) {
            p.setState(state);
            postgres.createProvision(p);
        }
        long result = postgres.getDesiredNumberOfVMs();
        // only the two in pending and running state should matter
        assertEquals(2, result);
    }

    /**
     * Test of getSuccessfulVMAddresses method, of class PostgreSQL.
     */
    @Test
    public void testGetSuccessfulVMAddresses() {
        System.out.println("getSuccessfulVMAddresses");
        Provision p = createProvision();
        p.setIpAddress("9.9.9.9");
        p.setState(ProvisionState.START);
        postgres.createProvision(p);
        p.setIpAddress("9.9.9.8");
        p.setState(ProvisionState.SUCCESS);
        postgres.createProvision(p);
        p.setIpAddress("9.9.9.7");
        p.setState(ProvisionState.SUCCESS);
        postgres.createProvision(p);
        String[] result = postgres.getSuccessfulVMAddresses();
        Assert.assertTrue("found addresses, incorrect number" + result.length, result.length == 2);
    }

    /**
     * Test of updateJobMessage method, of class PostgreSQL.
     */
    @Test
    public void testUpdateJobMessage() {
        System.out.println("updateJobMessage");
        Job createJob = createJob();
        createJob.setState(JobState.START);
        String uuid = postgres.createJob(createJob);
        postgres.updateJobMessage(uuid, "oof", "oh");
        List<Job> jobs = postgres.getJobs(JobState.START);
        Assert.assertTrue("job stdout and stderr incorrect", jobs.get(0).getStdout().equals("oof") && jobs.get(0).getStderr().equals("oh"));
    }

    /**
     * Test of getProvisions method, of class PostgreSQL.
     */
    @Test
    public void testGetProvisions() {
        System.out.println("getProvisions");
        Provision p = createProvision();
        p.setIpAddress("9.9.9.9");
        p.setState(ProvisionState.START);
        postgres.createProvision(p);
        p.setIpAddress("9.9.9.8");
        p.setState(ProvisionState.SUCCESS);
        postgres.createProvision(p);
        // make sure we get the latest one
        p.setIpAddress("1.1.1.1");
        p.setState(ProvisionState.FAILED);
        postgres.createProvision(p);
        p.setIpAddress("1.1.1.1");
        p.setState(ProvisionState.RUNNING);
        postgres.createProvision(p);

        List<Provision> result = postgres.getProvisions(ProvisionState.START);
        Assert.assertTrue("found START addresses, incorrect number " + result.size(), result.size() == 1);

        result = postgres.getProvisions(ProvisionState.FAILED);
        Assert.assertTrue("found FAILED addresses, incorrect number " + result.size(), result.isEmpty());

        result = postgres.getProvisions(ProvisionState.RUNNING);
        Assert.assertTrue("found RUNNING addresses, incorrect number " + result.size(), result.size() == 1);
    }
}
