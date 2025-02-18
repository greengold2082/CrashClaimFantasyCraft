package net.crashcraft.crashclaim.claimobjects;

public class Contribution {
    private int area;
    private double price;

    public Contribution (int area, double price) {
        this.area = area;
        this.price = price;
    }

    public int getArea() {
        return this.area;
    }

    public double getPrice() {
        return this.price;
    }
}
