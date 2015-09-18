package info.consonance.arch.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author boconnor
 * @author dyuen
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="job_id")
    private long jobId;
    private JobState state = JobState.START;
    private String uuid = UUID.randomUUID().toString().toLowerCase();
    private String workflow;
    private String vmUuid;
    private String workflowVersion;
    private String workflowPath;
    private String jobHash;
    private String messageType;
    private Map<String, String> ini = new HashMap<>();
    private Map<String, String> extraFiles = new HashMap<>();
    private Timestamp createTs;
    private Timestamp updateTs;
    private String stdout;
    private String stderr;
    private String endUser;
    private String flavour = null;

    public Job(String workflow, String workflowVersion, String workflowPath, String jobHash, Map<String, String> ini) {
        this.workflow = workflow;
        this.workflowVersion = workflowVersion;
        this.workflowPath = workflowPath;
        this.jobHash = jobHash;
        this.ini = ini;
    }

    public Job() {
        super();
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Job fromJSON(String json) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            return mapper.readValue(json, Job.class);
        } catch (JsonParseException e) {
            // TODO: improve logging for JSON parse errors.
            System.out.println("JSON parsing error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }

    }

    @JsonProperty
    public String getEndUser() {
        return endUser;
    }

    public void setEndUser(String endUser) {
        this.endUser = endUser;
    }

    @JsonProperty
    public String getFlavour() {
        return flavour;
    }

    public void setFlavour(String flavour) {
        this.flavour = flavour;
    }

    @JsonProperty("job_uuid")
    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @JsonProperty("arguments")
    public Map<String, String> getIni() {
        return ini;
    }

    @JsonProperty("extra_files")
    public Map<String, String> getExtraFiles() {
        return extraFiles;
    }

    @JsonIgnore
    public String getExtraFilesStr() {
        StringBuilder sb = new StringBuilder();
        for (String key : this.extraFiles.keySet()) {
            sb.append(key).append("=").append(this.extraFiles.get(key)).append("\n");
        }
        return (sb.toString());
    }

    public void setExtraFiles(Map<String, String> ini) {
        this.extraFiles = ini;
    }

    @JsonIgnore
    public String getIniStr() {
        StringBuilder sb = new StringBuilder();
        for (String key : this.ini.keySet()) {
            sb.append(key).append("=").append(this.ini.get(key)).append("\n");
        }
        return (sb.toString());
    }

    public void setIni(Map<String, String> ini) {
        this.ini = ini;
    }

    @JsonProperty("job_hash")
    public String getJobHash() {
        return jobHash;
    }

    public void setJobHash(String jobHash) {
        this.jobHash = jobHash;
    }

    @JsonProperty("workflow_version")
    public String getWorkflowVersion() {
        return workflowVersion;
    }

    public void setWorkflowVersion(String workflowVersion) {
        this.workflowVersion = workflowVersion;
    }

    @JsonProperty("workflow_name")
    public String getWorkflow() {
        return workflow;
    }

    public void setWorkflow(String workflow) {
        this.workflow = workflow;
    }

    // Not sure what this was for, ignore it for now.
    @JsonIgnore
    public JobState getState() {
        return state;
    }

    public void setState(JobState state) {
        this.state = state;
    }

    @JsonProperty("workflow_path")
    public String getWorkflowPath() {
        return workflowPath;
    }

    public void setWorkflowPath(String workflowPath) {
        this.workflowPath = workflowPath;
    }

    @JsonProperty("message_type")
    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    @JsonProperty("create_ts")
    public Timestamp getCreateTs() {
        return createTs;
    }

    public void setCreateTs(Timestamp createTs) {
        this.createTs = createTs;
    }

    @JsonProperty("update_ts")
    public Timestamp getUpdateTs() {
        return updateTs;
    }

    public void setUpdateTs(Timestamp updateTs) {
        this.updateTs = updateTs;
    }

    @JsonProperty("vmuuid")
    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    /**
     * @return the stdout
     */
    public String getStdout() {
        return stdout;
    }

    /**
     * @param stdout
     *            the stdout to set
     */
    public void setStdout(String stdout) {
        this.stdout = stdout;
    }

    /**
     * @return the stderr
     */
    public String getStderr() {
        return stderr;
    }

    /**
     * @param stderr
     *            the stderr to set
     */
    public void setStderr(String stderr) {
        this.stderr = stderr;
    }

    @JsonProperty("job_id")
    public long getJobId() {
        return jobId;
    }
}
