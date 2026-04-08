package com.example.firebase.models;

import java.io.Serializable;

public class Showtime implements Serializable {
    private String id;
    private String movieId;
    private String theaterName;
    private String time; // e.g., "19:00"
    private String date; // e.g., "2023-10-25"

    public Showtime() {}

    public Showtime(String id, String movieId, String theaterName, String time, String date) {
        this.id = id;
        this.movieId = movieId;
        this.theaterName = theaterName;
        this.time = time;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getMovieId() { return movieId; }
    public void setMovieId(String movieId) { this.movieId = movieId; }

    public String getTheaterName() { return theaterName; }
    public void setTheaterName(String theaterName) { this.theaterName = theaterName; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    @Override
    public String toString() {
        return theaterName + " - " + time;
    }
}
