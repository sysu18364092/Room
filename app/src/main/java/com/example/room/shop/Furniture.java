package com.example.room.shop;

public class Furniture {
    private int furnitureId;
    private String name ;
    private String oName ;
    private int imageId ;
    private String description;
    private int price ;
    private String priceType ;

    public Furniture(int furnitureId, String name, String oName, int imageId, String description, int price, String priceType) {
        this.furnitureId = furnitureId;
        this.name = name;
        this.oName = oName;
        this.imageId = imageId;
        this.description = description;
        this.price = price;
        this.priceType = priceType;
    }

    public int getFurnitureId() { return furnitureId; }

    public String getName() {
        return name;
    }

    public String getOName() {
        return oName;
    }

    public int getImageId() {
        return imageId;
    }

    public String getDescription() {
        return description;
    }

    public int getPrice() {
        return price;
    }

    public String getPriceType() {
        return priceType;
    }

    public String getPrinceString() {
        return price+"";
    }
}
