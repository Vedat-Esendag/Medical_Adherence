package com.example.medicaladherence.util

import com.example.medicaladherence.data.model.PatientDataExport
import com.example.medicaladherence.utils.AppConstants

object QRCodeScanner {
    
    /**
     * Parse and validate scanned QR code data
     * @param qrData Raw string data from QR code
     * @return PatientDataExport if valid, null otherwise
     */
    fun parseQRData(qrData: String): Result<PatientDataExport> {
        return try {
            val patientData = PatientDataExport.fromJson(qrData)
            
            if (patientData == null) {
                Result.failure(InvalidQRDataException("Invalid QR code format"))
            } else if (!isValidPatientData(patientData)) {
                Result.failure(InvalidQRDataException("Invalid patient data"))
            } else {
                Result.success(patientData)
            }
        } catch (e: Exception) {
            Result.failure(InvalidQRDataException("Failed to parse QR data: ${e.message}"))
        }
    }
    
    /**
     * Validate patient data from QR code
     */
    private fun isValidPatientData(data: PatientDataExport): Boolean {
        // Check PIN format
        if (data.pin.length != AppConstants.PIN_LENGTH || !data.pin.all { it.isDigit() }) {
            return false
        }
        
        // Check name is not blank
        if (data.name.isBlank()) {
            return false
        }
        
        // Data is valid
        return true
    }
}

class InvalidQRDataException(message: String) : Exception(message)

