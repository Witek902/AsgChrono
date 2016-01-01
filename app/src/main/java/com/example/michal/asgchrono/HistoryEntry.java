package com.example.michal.asgchrono;

public class HistoryEntry {
    public String name;
    public double velocity;
    public double fireRate;

    HistoryEntry(String name, double velocity, double fireRate) {
        this.name = name;
        this.velocity = velocity;
        this.fireRate = fireRate;
    }

    public String toString()
    {
        return name + " " + Double.toString(velocity) + " " + Double.toString(fireRate);
    }
}
