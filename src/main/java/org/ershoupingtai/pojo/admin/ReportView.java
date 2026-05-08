package org.ershoupingtai.pojo.admin;

public class ReportView {
    private Integer reportId;
    private Integer goodsId;
    private String goodsName;
    private String goodsLocation;
    private Integer reporterId;
    private String reporterStudentId;
    private Integer reportedId;
    private String reportedStudentId;
    private Integer type;
    private String typeText;
    private String description;
    private Boolean isHandled;
    private String createdAt;

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public String getGoodsLocation() {
        return goodsLocation;
    }

    public void setGoodsLocation(String goodsLocation) {
        this.goodsLocation = goodsLocation;
    }

    public Integer getReporterId() {
        return reporterId;
    }

    public void setReporterId(Integer reporterId) {
        this.reporterId = reporterId;
    }

    public String getReporterStudentId() {
        return reporterStudentId;
    }

    public void setReporterStudentId(String reporterStudentId) {
        this.reporterStudentId = reporterStudentId;
    }

    public Integer getReportedId() {
        return reportedId;
    }

    public void setReportedId(Integer reportedId) {
        this.reportedId = reportedId;
    }

    public String getReportedStudentId() {
        return reportedStudentId;
    }

    public void setReportedStudentId(String reportedStudentId) {
        this.reportedStudentId = reportedStudentId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTypeText() {
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsHandled() {
        return isHandled;
    }

    public void setIsHandled(Boolean isHandled) {
        this.isHandled = isHandled;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
