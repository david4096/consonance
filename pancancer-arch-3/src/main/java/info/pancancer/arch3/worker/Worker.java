package info.pancancer.arch3.worker;

import info.pancancer.arch3.beans.Job;
import info.pancancer.arch3.beans.Status;
import info.pancancer.arch3.utils.Utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;

/**
 * Created by boconnor on 15-04-18.
 */
public class Worker implements Runnable {

    private static final String CHARSET_ENCODING = "UTF-8";
    private static final int THREAD_POOL_SIZE = 1;
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private JSONObject settings = null;
    private Channel resultsChannel = null;
    private Channel jobChannel = null;
    private Connection connection = null;
    private String queueName = null;
    private Utilities u = new Utilities();
    private String vmUuid = null;

    public static void main(String[] argv) throws Exception {

        OptionParser parser = new OptionParser();
        parser.accepts("config").withOptionalArg().ofType(String.class);
        parser.accepts("uuid").withOptionalArg().ofType(String.class);
        OptionSet options = parser.parse(argv);

        String configFile = null;
        String uuid = null;
        if (options.has("config")) {
            configFile = (String) options.valueOf("config");
        }
        if (options.has("uuid")) {
            uuid = (String) options.valueOf("uuid");
        }

        // TODO: can't run on the command line anymore!
        /*
         * Worker w = new Worker(configFile, uuid); ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
         * ExecutorCompletionService<Object> execService = new ExecutorCompletionService<Object>(pool); Object result = new Object();
         * execService.submit(w, result); while(execService.poll()!=null) { //working... } pool.shutdownNow();
         */
        Worker w = new Worker(configFile, uuid);
        w.run();
        System.out.println("Exiting.");
    }

    public Worker(String configFile, String vmUuid) {

        settings = u.parseConfig(configFile);
        queueName = (String) settings.get("rabbitMQQueueName");
        this.vmUuid = vmUuid;

    }

    @Override
    public void run() {

        int max = 1;

        try {

            // the VM UUID
            System.out.println(" WORKER VM UUID: '" + vmUuid + "'");

            // read from

            jobChannel = u.setupQueue(settings, queueName + "_jobs");
            // write to
            resultsChannel = u.setupMultiQueue(settings, queueName + "_results");

            QueueingConsumer consumer = new QueueingConsumer(jobChannel);
            String consumerTag = jobChannel.basicConsume(queueName + "_jobs", true, consumer);

            // TODO: need threads that each read from orders and another that reads results
            while (max > 0) {

                System.out.println(" WORKER IS PREPARING TO PULL JOB FROM QUEUE WITH NAME: " + queueName + "_jobs");

                // loop once
                // TODO: this will be configurable so it could process multiple jobs before exiting

                // get the job order
                // int messages = jobChannel.queueDeclarePassive(queueName + "_jobs").getMessageCount();
                // System.out.println("THERE ARE CURRENTLY "+messages+" JOBS QUEUED!");

                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                // jchannel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                String message;
                message = new String(delivery.getBody());

                if ("".equals(message) || message.length() > 0) {

                    max--;

                    System.out.println(" [x] Received JOBS REQUEST '" + message + "'");

                    Job job = new Job().fromJSON(message);

                    // TODO: this will obviously get much more complicated when integrated with Docker
                    // launch VM
                    Status status = new Status(vmUuid, job.getUuid(), u.RUNNING, u.JOB_MESSAGE_TYPE, "job is starting");
                    String statusJSON = status.toJSON();

                    System.out.println(" WORKER LAUNCHING JOB");
                    // TODO: this is where I would create an INI file and run the local command to run a seqware workflow, in it's own
                    // thread, harvesting STDERR/STDOUT periodically
                    launchJob(statusJSON, job);

                    // FIXME: this is the source of the bug... this thread never exists and as a consequence it uses the
                    // same VMUUID for all jobs... which mismatches what's in the DB and hence the update in the DB never happens

                    status = new Status(vmUuid, job.getUuid(), u.SUCCESS, u.JOB_MESSAGE_TYPE, "job is finished");
                    statusJSON = status.toJSON();

                    System.out.println(" WORKER FINISHING JOB");

                    finishJob(statusJSON);
                } else {
                    System.out.println(" [x] Job request came back NULL! ");
                }

            }
            jobChannel.getConnection().close();
            resultsChannel.getConnection().close();
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();
        }
        // this.notify();
    }

    private Path writeINIFile(Job job) throws IOException {
        System.out.println("INI is: " + job.getIniStr());

        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        perms.add(PosixFilePermission.OWNER_EXECUTE);
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        perms.add(PosixFilePermission.GROUP_EXECUTE);
        perms.add(PosixFilePermission.OTHERS_READ);
        perms.add(PosixFilePermission.OTHERS_WRITE);
        perms.add(PosixFilePermission.OTHERS_EXECUTE);
        FileAttribute<?> attrs = PosixFilePermissions.asFileAttribute(perms);
        Path pathToINI = java.nio.file.Files.createTempFile("seqware_", ".ini", attrs);
        System.out.println("INI file: " + pathToINI.toString());
        FileWriter writer = new FileWriter(pathToINI.toFile());
        writer.write(job.getIniStr());
        writer.close();
        return pathToINI;
    }

    // TODO: obviously, this will need to launch something using Youxia in the future
    private void launchJob(String message, Job job) {
        String cmdResponse = null;
        /*LogOutputStream outputStream = new LogOutputStream() {
            private final List<String> lines = new LinkedList<String>();
            @Override protected void processLine(String line, int level) {
                lines.add(line);
            }   
            public List<String> getLines() {
                return lines;
            }
            
            public String toString()
            {
                StringBuffer buff = new StringBuffer();
                for (String l : this.lines)
                {
                    buff.append(l);
                }
                return buff.toString();
            }
        };*/
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);

        try {
            Path pathToINI = writeINIFile(job);
            resultsChannel.basicPublish(queueName + "_results", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN,
                    message.getBytes());

            // Now we need to launch seqware in docker.
            DefaultExecutor executor = new DefaultExecutor();

            CommandLine cli = new CommandLine("docker");
            cli.addArguments(new String[] { "run", "--rm", "-h", "master", "-t", "-v", job.getWorkflowPath() + ":/workflow", "-v",
                    pathToINI + ":/ini", /*"-v","/datastore/:/datastore/",*/ "-i", "seqware/seqware_whitestar_pancancer", "seqware", "bundle", "launch", "--dir", "/workflow",
                    "--ini", "/ini", "--no-metadata" });
            System.out.println("Executing command: " + cli.toString());


            executor.setStreamHandler(streamHandler);
            executor.execute(cli);
            outputStream.flush();
            //cmdResponse = outputStream.getLines();//outputStream.toString(CHARSET_ENCODING);
            System.out.println("Docker execution result: " + outputStream.toString());
            outputStream.close();

        } catch (IOException e) {
            // if (cmdResponse!=null)
            // cmdResponse.toString();
            if (outputStream != null) {
                log.error("Error from Docker: " + outputStream.toString());
            }
            e.printStackTrace();
            log.error(e.toString());
        }

    }

    private void finishJob(String message) {
        try {
            resultsChannel.basicPublish("", queueName + "_results", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes());
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

}
