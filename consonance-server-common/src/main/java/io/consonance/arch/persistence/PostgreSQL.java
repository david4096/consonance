package io.consonance.arch.persistence;

import io.consonance.arch.beans.Job;
import io.consonance.arch.beans.JobState;
import io.consonance.arch.beans.Provision;
import io.consonance.arch.beans.ProvisionState;
import io.consonance.common.BasicPostgreSQL;
import io.consonance.arch.utils.CommonServerTestUtilities;
import org.apache.commons.configuration.HierarchicalINIConfiguration;
import org.apache.commons.dbutils.handlers.ArrayHandler;
import org.apache.commons.dbutils.handlers.KeyedHandler;
import org.apache.commons.dbutils.handlers.ScalarHandler;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author oconnor
 * @author dyuen
 */
public class PostgreSQL extends BasicPostgreSQL{

    protected static final Logger LOG = LoggerFactory.getLogger(PostgreSQL.class);

    public PostgreSQL(HierarchicalINIConfiguration settings) {
        super(settings);
    }

    public long getDesiredNumberOfVMs() {
        return runSelectStatement("select count(*) from provision where status = '" + ProvisionState.PENDING + "' or status = '"
                + ProvisionState.RUNNING + "'", new ScalarHandler<>());
    }

    public String getPendingProvisionUUID() {
        return runSelectStatement("select provision_uuid from provision where status = '" + ProvisionState.PENDING + "' limit 1",
                new ScalarHandler<>());
    }



    public void updatePendingProvision(String uuid) {
        runUpdateStatement("update provision set status = ?, update_timestamp = NOW() where provision_uuid = ?",
                ProvisionState.RUNNING.toString(), uuid);
    }

    public void finishContainer(String uuid) {
        runUpdateStatement("update provision set status = ? , update_timestamp = NOW() where provision_uuid = ? ",
                ProvisionState.SUCCESS.toString(), uuid);
    }

    public void updateJobMessage(String uuid, String stdout, String stderr) {
        runUpdateStatement("update job set stdout = ?, stderr = ?, update_timestamp = NOW() where job_uuid = ?", stdout, stderr, uuid);
    }

    public void finishJob(String uuid) {
        runUpdateStatement("update job set status = ? , update_timestamp = NOW() where job_uuid = ?", JobState.SUCCESS.toString(), uuid);
    }

    public void updateJob(String uuid, String vmUuid, JobState status) {
        runUpdateStatement("update job set status = ?, provision_uuid = ?, update_timestamp = NOW() where job_uuid = ?", status.toString(),
                vmUuid, uuid);
    }

    public void updateProvisionByProvisionUUID(String provisionUuid, String jobUuid, ProvisionState status, String ipAddress) {
        runUpdateStatement(
                "update provision set status = ? , job_uuid = ? , update_timestamp = NOW(), ip_address = ? where provision_uuid = ?",
                status.toString(), jobUuid, ipAddress, provisionUuid);
    }

    public void updateProvisionByJobUUID(String jobUUID, String provisionUUID, ProvisionState status, String ipAddress) {
        runUpdateStatement(
                "update provision set status = ? , provision_uuid = ?, update_timestamp = NOW(), ip_address = ? where job_uuid = ?",
                status.toString(), provisionUUID, ipAddress, jobUUID);
    }

    public long getProvisionCount(ProvisionState status) {
        return this.runSelectStatement("select count(*) from provision where status = ?", new ScalarHandler<>(), status.toString());
    }

    public Integer createProvision(Provision p) {
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO provision (status, provision_uuid, cores, mem_gb, storage_gb, job_uuid, ip_address) VALUES (?,?,?,?,?,?,?)",
                new KeyedHandler<>("provision_id"), p.getState().toString(), p.getProvisionUUID(), p.getCores(), p.getMemGb(),
                p.getStorageGb(), p.getJobUUID(), p.getIpAddress());
        return (Integer) map.entrySet().iterator().next().getKey();
    }

    public String createJob(Job j) {
        JSONObject jsonIni = new JSONObject(j.getIni());
        Map<Object, Map<String, Object>> map = this.runInsertStatement(
                "INSERT INTO job (status, job_uuid, workflow, workflow_version, job_hash, ini) VALUES (?,?,?,?,?,?)",
                new KeyedHandler<>("job_uuid"), j.getState().toString(), j.getUuid(), j.getWorkflow(), j.getWorkflowVersion(),
                j.getJobHash(), jsonIni.toJSONString());
        return (String) map.entrySet().iterator().next().getKey();
    }

    public String[] getSuccessfulVMAddresses() {
        Map<String, Map<String, Object>> runSelectStatement = runSelectStatement(
                "select provision_id, ip_address from provision where status = '" + ProvisionState.SUCCESS + "'", new KeyedHandler<>(
                        "provision_id"));
        List<String> list = runSelectStatement.entrySet().stream().map(entry -> (String) entry.getValue().get("ip_address"))
                .collect(Collectors.toList());
        return list.toArray(new String[list.size()]);
    }

    public List<Provision> getProvisions(ProvisionState status) {

        List<Provision> provisions = new ArrayList<>();
        Map<Object, Map<String, Object>> map;
        if (status != null) {
            map = this
                    .runSelectStatement(
                            "select * from provision where provision_id in (select max(provision_id) from provision group by ip_address) and status = ?",
                            new KeyedHandler<>("provision_uuid"), status.toString());
        } else {
            map = this.runSelectStatement(
                    "select * from provision where provision_id in (select max(provision_id) from provision group by ip_address)",
                    new KeyedHandler<>("provision_uuid"));
        }

        //TODO: this can be done more cleanly with a custom row processor
        for (Entry<Object, Map<String, Object>> entry : map.entrySet()) {
            Provision p = new Provision();
            p.setState(Enum.valueOf(ProvisionState.class, (String) entry.getValue().get("status")));
            p.setJobUUID((String) entry.getValue().get("job_uuid"));
            p.setProvisionUUID((String) entry.getValue().get("provision_uuid"));
            p.setIpAddress((String) entry.getValue().get("ip_address"));
            p.setCores((Integer) entry.getValue().get("cores"));
            p.setMemGb((Integer) entry.getValue().get("mem_gb"));
            p.setStorageGb((Integer) entry.getValue().get("storage_gb"));

            // timestamp
            Timestamp createTs = (Timestamp) entry.getValue().get("create_timestamp");
            Timestamp updateTs = (Timestamp) entry.getValue().get("update_timestamp");
            p.setCreateTimestamp(createTs);
            p.setUpdateTimestamp(updateTs);

            provisions.add(p);

        }

        return provisions;
    }

    public List<Job> getJobs(JobState status) {

        List<Job> jobs = new ArrayList<>();
        Map<Object, Map<String, Object>> map;
        if (status != null) {
            map = this.runSelectStatement("select * from job where status = ?", new KeyedHandler<>("job_uuid"), status.toString());
        } else {
            map = this.runSelectStatement("select * from job", new KeyedHandler<>("job_uuid"));
        }

        //TODO: this can be done more cleanly with a custom row processor
        for (Entry<Object, Map<String, Object>> entry : map.entrySet()) {

            Job j = new Job();
            j.setState(Enum.valueOf(JobState.class, (String) entry.getValue().get("status")));
            j.setUuid((String) entry.getValue().get("job_uuid"));
            j.setWorkflow((String) entry.getValue().get("workflow"));
            j.setWorkflowVersion((String) entry.getValue().get("workflow_version"));
            j.setJobHash((String) entry.getValue().get("job_hash"));
            j.setStdout((String) entry.getValue().get("stdout"));
            j.setStderr((String) entry.getValue().get("stderr"));
            final Map<String, String> ini = convertJSON(entry, "ini");
            j.setIni(ini);

            // timestamp
            Timestamp createTs = (Timestamp) entry.getValue().get("create_timestamp");
            Timestamp updateTs = (Timestamp) entry.getValue().get("update_timestamp");
            j.setCreateTimestamp(createTs);
            j.setUpdateTimestamp(updateTs);

            jobs.add(j);

        }

        return jobs;
    }

    private Map<String, String> convertJSON(Entry<Object, Map<String, Object>> entry, String columnName) {
        JSONObject iniJson = CommonServerTestUtilities.parseJSONStr(entry.getValue().get(columnName).toString());
        HashMap<String, String> ini = new HashMap<>();
        for (Object key : iniJson.keySet()) {
            ini.put((String) key, (String) iniJson.get(key));
        }
        return ini;
    }

    public boolean previouslyRun(String hash) {
        Object[] runSelectStatement = this.runSelectStatement(
                "select * from job where job_hash = ? and status !='" + JobState.FAILED.toString() + "' and status != '" + JobState.LOST
                        + "'", new ArrayHandler(), hash);
        return (runSelectStatement.length > 0);
    }

}
