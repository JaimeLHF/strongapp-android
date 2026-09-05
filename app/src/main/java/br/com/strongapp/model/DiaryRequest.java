package br.com.strongapp.model;

public class DiaryRequest {
    public String notes;
    public Integer rating;

    public DiaryRequest(String notes, Integer rating) {
        this.notes = notes;
        this.rating = rating;
    }
}
