package org.ershoupingtai.service;

import org.ershoupingtai.mapper.GoodsMapper;
import org.ershoupingtai.pojo.Goods;
import org.ershoupingtai.pojo.ReportRequest;
import org.ershoupingtai.pojo.admin.ReportView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ReportService {
    private final JdbcTemplate jdbcTemplate;
    private final GoodsMapper goodsMapper;

    public ReportService(JdbcTemplate jdbcTemplate, GoodsMapper goodsMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.goodsMapper = goodsMapper;
    }

    @PostConstruct
    public void initTable() {
        jdbcTemplate.execute("IF OBJECT_ID('dbo.Report', 'U') IS NULL BEGIN " +
                "CREATE TABLE dbo.Report (" +
                "ReportId INT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                "GoodsId INT NOT NULL, " +
                "ReporterId INT NOT NULL, " +
                "ReportedId INT NOT NULL, " +
                "Type INT NULL, " +
                "Description NVARCHAR(500) NULL, " +
                "IsHandled BIT NOT NULL CONSTRAINT DF_Report_IsHandled DEFAULT(0), " +
            "created_at DATETIME2 NOT NULL CONSTRAINT DF_Report_created_at DEFAULT(SYSDATETIME())" +
                ") END");

        jdbcTemplate.execute("IF OBJECT_ID('dbo.Report', 'U') IS NOT NULL AND COL_LENGTH('dbo.Report', 'created_at') IS NULL " +
            "BEGIN ALTER TABLE dbo.Report ADD created_at DATETIME2 NOT NULL CONSTRAINT DF_Report_created_at DEFAULT(SYSDATETIME()) WITH VALUES END");
    }

    public void submitReport(Integer reporterId, Long goodsId, ReportRequest request) {
        if (reporterId == null || reporterId <= 0) {
            throw new IllegalArgumentException("请先登录");
        }
        if (goodsId == null || goodsId <= 0) {
            throw new IllegalArgumentException("商品ID无效");
        }
        Goods goods = goodsMapper.selectByIdWithSellerStudentId(goodsId);
        if (goods == null || goods.getGoodsId() == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        if (goods.getUserId() == null) {
            throw new IllegalArgumentException("商品卖家信息缺失");
        }
        if (reporterId.intValue() == goods.getUserId()) {
            throw new IllegalArgumentException("不能举报自己的商品");
        }

        String description = request == null ? null : request.getDescription();
        String normalizedDescription = description == null ? "" : description.trim();
        if (normalizedDescription.length() > 500) {
            normalizedDescription = normalizedDescription.substring(0, 500);
        }

        Integer reportType = request == null ? null : request.getType();
        jdbcTemplate.update(
            "INSERT INTO dbo.Report (GoodsId, ReporterId, ReportedId, Type, Description, IsHandled, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)",
                goods.getGoodsId(),
                reporterId,
                goods.getUserId(),
                reportType,
                normalizedDescription,
                false,
                new Timestamp(System.currentTimeMillis())
        );
    }

    public List<ReportView> listReports() {
        return jdbcTemplate.query(
            "SELECT r.ReportId, r.GoodsId, r.ReporterId, r.ReportedId, r.Type, r.Description, r.IsHandled, r.created_at, " +
                        "g.GoodsName, g.GoodsLocation, " +
                        "ru.UserName AS ReporterStudentId, " +
                        "su.UserName AS ReportedStudentId " +
                        "FROM dbo.Report r " +
                        "LEFT JOIN dbo.Goods g ON g.GoodsId = r.GoodsId " +
                        "LEFT JOIN dbo.UserLogin ru ON ru.UserId = r.ReporterId " +
                        "LEFT JOIN dbo.UserLogin su ON su.UserId = r.ReportedId " +
                "ORDER BY r.created_at DESC, r.ReportId DESC",
                new RowMapper<ReportView>() {
                    @Override
                    public ReportView mapRow(ResultSet rs, int rowNum) throws SQLException {
                        ReportView view = new ReportView();
                        view.setReportId(rs.getInt("ReportId"));
                        view.setGoodsId(rs.getInt("GoodsId"));
                        view.setReporterId(rs.getInt("ReporterId"));
                        view.setReportedId(rs.getInt("ReportedId"));
                        view.setType((Integer) rs.getObject("Type"));
                        view.setDescription(rs.getString("Description"));
                        view.setIsHandled(rs.getBoolean("IsHandled"));
                        view.setGoodsName(rs.getString("GoodsName"));
                        view.setGoodsLocation(rs.getString("GoodsLocation"));
                        view.setReporterStudentId(rs.getString("ReporterStudentId"));
                        view.setReportedStudentId(rs.getString("ReportedStudentId"));
                        Timestamp createdAt = rs.getTimestamp("created_at");
                        if (createdAt != null) {
                            view.setCreatedAt(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(createdAt.getTime())));
                        }
                        view.setTypeText(resolveTypeText((Integer) rs.getObject("Type")));
                        return view;
                    }
                }
        );
    }

    public int markAsHandled(Integer reportId) {
        if (reportId == null || reportId <= 0) {
            throw new IllegalArgumentException("举报ID无效");
        }
        return jdbcTemplate.update("UPDATE dbo.Report SET IsHandled = 1 WHERE ReportId = ?", reportId);
    }

    private String resolveTypeText(Integer type) {
        if (type == null) {
            return "未分类";
        }
        switch (type) {
            case 1:
                return "商品信息不实";
            case 2:
                return "疑似违规商品";
            case 3:
                return "商品图片/描述不符";
            case 4:
                return "价格异常";
            default:
                return "其他";
        }
    }
}
