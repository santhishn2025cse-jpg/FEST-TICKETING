package com.festpass.dto;

public class EventHeadcountResponse {

    private Long eventId;
    private String eventName;
    private int capacity;
    private long totalTicketsIssued;
    private long currentHeadcount; // checked-in tickets count
    private int remainingCapacity;
    private double occupancyPercentage;

    public EventHeadcountResponse() {
    }

    public EventHeadcountResponse(Long eventId, String eventName, int capacity, long totalTicketsIssued, long currentHeadcount) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.capacity = capacity;
        this.totalTicketsIssued = totalTicketsIssued;
        this.currentHeadcount = currentHeadcount;
        this.remainingCapacity = capacity - (int) totalTicketsIssued;
        this.occupancyPercentage = capacity > 0 ? (double) currentHeadcount / capacity * 100.0 : 0.0;
    }

    public Long getEventId() {
        return eventId;
    }

    public void setEventId(Long eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public long getTotalTicketsIssued() {
        return totalTicketsIssued;
    }

    public void setTotalTicketsIssued(long totalTicketsIssued) {
        this.totalTicketsIssued = totalTicketsIssued;
    }

    public long getCurrentHeadcount() {
        return currentHeadcount;
    }

    public void setCurrentHeadcount(long currentHeadcount) {
        this.currentHeadcount = currentHeadcount;
    }

    public int getRemainingCapacity() {
        return remainingCapacity;
    }

    public void setRemainingCapacity(int remainingCapacity) {
        this.remainingCapacity = remainingCapacity;
    }

    public double getOccupancyPercentage() {
        return occupancyPercentage;
    }

    public void setOccupancyPercentage(double occupancyPercentage) {
        this.occupancyPercentage = occupancyPercentage;
    }
}
