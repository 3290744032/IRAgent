package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ExamFilterData {
    @SerializedName("years") private List<Integer> years;
    @SerializedName("examTypes") private List<String> examTypes;
    @SerializedName("subjects") private List<String> subjects;

    public List<Integer> getYears() { return years; }
    public List<String> getExamTypes() { return examTypes; }
    public List<String> getSubjects() { return subjects; }
}
