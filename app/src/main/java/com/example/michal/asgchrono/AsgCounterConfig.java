package com.example.michal.asgchrono;

public class AsgCounterConfig {
	
    // TODO: these should be in seconds
    public int minPeakDistance;  // minimum twin peaks distance (in samples)
    public int maxPeakDistance;  // maximum twin peaks distance (in samples)

    public float sampleRate;       // samples per second
    public float length;           // photocell length in meters

    public float mass;             // BB mass in kilograms

    public float detectionSigma;
    public float fireRateTreshold;

    public AsgCounterConfig()
    {
        sampleRate = 44100.0f;
        length = 0.2f;

        minPeakDistance = 30;
        maxPeakDistance = 300;

        mass = 0.0002f;
        fireRateTreshold = 1.25f;
        detectionSigma = 7.0f;
    }
}
