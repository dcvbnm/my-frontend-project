package org.ershoupingtai.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("Goods")
public class Goods {
	// 商品主键
    @TableId(value = "GoodsId", type = IdType.AUTO)
    private Integer goodsId;

    // 商品图片、名称、类型和描述
    @TableField("GoodsImage")
    private String goodsImage;

    @TableField("GoodsName")
    private String goodsName;

    @TableField("GoodsType")
    private Integer goodsType;

    @TableField("GoodsDescription")
    private String goodsDesc;

    @TableField("Price")
    private BigDecimal goodsPrice;

    @TableField("GoodsQuantity")
    private Integer goodsQuantity;

    @TableField("GoodsDate")
    private LocalDate goodsDate;

    @TableField("Shelflife")
    private Integer shelflife;

    @TableField("Views")
    private Integer views;

    @TableField("GoodsLocation")
    private String goodsLocation;

    @TableField("UserId")
    private Integer userId;

    @TableField(exist = false)
    private String sellerStudentId;

    @TableField("Stock")
    private Boolean stock;

    @TableField("IsDeleted")
    private Boolean isDeleted;

    public Integer getGoodsId() {
        return goodsId;
    }

    public void setGoodsId(Integer goodsId) {
        this.goodsId = goodsId;
    }

    public String getGoodsImage() {
        return goodsImage;
    }

    public void setGoodsImage(String goodsImage) {
        this.goodsImage = goodsImage;
    }

    public String getGoodsName() {
        return goodsName;
    }

    public void setGoodsName(String goodsName) {
        this.goodsName = goodsName;
    }

    public Integer getGoodsType() {
        return goodsType;
    }

    public void setGoodsType(Integer goodsType) {
        this.goodsType = goodsType;
    }

    public String getGoodsDesc() {
        return goodsDesc;
    }

    public void setGoodsDesc(String goodsDesc) {
        this.goodsDesc = goodsDesc;
    }

    public BigDecimal getGoodsPrice() {
        return goodsPrice;
    }

    public void setGoodsPrice(BigDecimal goodsPrice) {
        this.goodsPrice = goodsPrice;
    }

    public Integer getGoodsQuantity() {
        return goodsQuantity;
    }

    public void setGoodsQuantity(Integer goodsQuantity) {
        this.goodsQuantity = goodsQuantity;
    }

    public LocalDate getGoodsDate() {
        return goodsDate;
    }

    public void setGoodsDate(LocalDate goodsDate) {
        this.goodsDate = goodsDate;
    }

    public Integer getShelflife() {
        return shelflife;
    }

    public void setShelflife(Integer shelflife) {
        this.shelflife = shelflife;
    }

    public Integer getViews() {
        return views;
    }

    public void setViews(Integer views) {
        this.views = views;
    }

    public String getGoodsLocation() {
        return goodsLocation;
    }

    public void setGoodsLocation(String goodsLocation) {
        this.goodsLocation = goodsLocation;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getSellerStudentId() {
        return sellerStudentId;
    }

    public void setSellerStudentId(String sellerStudentId) {
        this.sellerStudentId = sellerStudentId;
    }

    public Boolean getStock() {
        return stock;
    }

    public void setStock(Boolean stock) {
        this.stock = stock;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean deleted) {
        isDeleted = deleted;
    }
}
