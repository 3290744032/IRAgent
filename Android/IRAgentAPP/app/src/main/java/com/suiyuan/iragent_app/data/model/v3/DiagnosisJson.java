package com.suiyuan.iragent_app.data.model.v3;

import com.google.gson.annotations.SerializedName;

public class DiagnosisJson {
    @SerializedName("formula_confusion")
    private DiagnosisItem formulaConfusion;
    @SerializedName("calculation_error")
    private DiagnosisItem calculationError;
    @SerializedName("prerequisite_check")
    private DiagnosisItem prerequisiteCheck;

    public DiagnosisItem getFormulaConfusion() { return formulaConfusion; }
    public void setFormulaConfusion(DiagnosisItem formulaConfusion) { this.formulaConfusion = formulaConfusion; }
    public DiagnosisItem getCalculationError() { return calculationError; }
    public void setCalculationError(DiagnosisItem calculationError) { this.calculationError = calculationError; }
    public DiagnosisItem getPrerequisiteCheck() { return prerequisiteCheck; }
    public void setPrerequisiteCheck(DiagnosisItem prerequisiteCheck) { this.prerequisiteCheck = prerequisiteCheck; }
}
