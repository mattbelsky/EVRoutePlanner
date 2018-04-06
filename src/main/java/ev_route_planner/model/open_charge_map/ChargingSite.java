package ev_route_planner.model.open_charge_map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ChargingSite {

    @JsonProperty("ID")
    int id;
    @JsonProperty("UUID")
    String uuid;
    @JsonProperty("DataProviderID")
    int dataProviderID;
    @JsonProperty("OperatorID")
    int operatorID;
    @JsonProperty("OperatorsReference")
    String operatorsReference;
    @JsonProperty("UsageTypeID")
    int usageTypeID;
    @JsonProperty("UsageCost")
    String usageCost;
    @JsonProperty("AddressInfo")
    AddressInfo addressInfo;
    @JsonProperty("NumberOfPoints")
    int numberOfPoints;
    @JsonProperty("GeneralComments")
    String generalComments;
    @JsonProperty("StatusTypeID")
    int statusTypeID;
    @JsonProperty("DateLastStatusUpdate")
    String dateLastStatusUpdate;
    @JsonProperty("DataQualityLevel")
    int dataQualityLevel;
    @JsonProperty("DateCreated")
    String dateCreated;
    @JsonProperty("SubmissionStatusTypeID")
    int submissionStatusTypeID;
    @JsonProperty("Connections")
    Connections[] connections;
    @JsonProperty("IsRecentlyVerified")
    boolean recentlyVerified;
    @JsonProperty("DateLastVerified")
    String dateLastVerified;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getDataProviderID() {
        return dataProviderID;
    }

    public void setDataProviderID(int dataProviderID) {
        this.dataProviderID = dataProviderID;
    }

    public int getOperatorID() {
        return operatorID;
    }

    public void setOperatorID(int operatorID) {
        this.operatorID = operatorID;
    }

    public String getOperatorsReference() {
        return operatorsReference;
    }

    public void setOperatorsReference(String operatorsReference) {
        this.operatorsReference = operatorsReference;
    }

    public int getUsageTypeID() {
        return usageTypeID;
    }

    public void setUsageTypeID(int usageTypeID) {
        this.usageTypeID = usageTypeID;
    }

    public String getUsageCost() {
        return usageCost;
    }

    public void setUsageCost(String usageCost) {
        this.usageCost = usageCost;
    }

    public AddressInfo getAddressInfo() {
        return addressInfo;
    }

    public void setAddressInfo(AddressInfo addressInfo) {
        this.addressInfo = addressInfo;
    }

    public int getNumberOfPoints() {
        return numberOfPoints;
    }

    public void setNumberOfPoints(int numberOfPoints) {
        this.numberOfPoints = numberOfPoints;
    }

    public String getGeneralComments() {
        return generalComments;
    }

    public void setGeneralComments(String generalComments) {
        this.generalComments = generalComments;
    }

    public int getStatusTypeID() {
        return statusTypeID;
    }

    public void setStatusTypeID(int statusTypeID) {
        this.statusTypeID = statusTypeID;
    }

    public String getDateLastStatusUpdate() {
        return dateLastStatusUpdate;
    }

    public void setDateLastStatusUpdate(String dateLastStatusUpdate) {
        this.dateLastStatusUpdate = dateLastStatusUpdate;
    }

    public int getDataQualityLevel() {
        return dataQualityLevel;
    }

    public void setDataQualityLevel(int dataQualityLevel) {
        this.dataQualityLevel = dataQualityLevel;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public int getSubmissionStatusTypeID() {
        return submissionStatusTypeID;
    }

    public void setSubmissionStatusTypeID(int submissionStatusTypeID) {
        this.submissionStatusTypeID = submissionStatusTypeID;
    }

    public Connections[] getConnections() {
        return connections;
    }

    public void setConnections(Connections[] connections) {
        this.connections = connections;
    }

    public boolean isRecentlyVerified() {
        return recentlyVerified;
    }

    public void setRecentlyVerified(boolean recentlyVerified) {
        this.recentlyVerified = recentlyVerified;
    }

    public String getDateLastVerified() {
        return dateLastVerified;
    }

    public void setDateLastVerified(String dateLastVerified) {
        this.dateLastVerified = dateLastVerified;
    }
}
