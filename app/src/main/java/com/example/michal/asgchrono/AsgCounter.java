package com.example.michal.asgchrono;

public class AsgCounter {
	
    static final int BUFFER_SIZE = 8192;
    
	// for Hermite interpolation
    static final int HISTORY_SAMPLES_BEFORE = 2;

    AsgCounterConfig config;
    AsgStats stats;

    boolean warmup;
    float averageRMS;
    AsgCounterState state;
    float peakSearchStart[];

    float buffer[];
    int bufferPtr;

    int samplePos;  // samples passed since Reset()
    int sampleInCurState;  // samples passed since last state change

    int reportsNum;
    float prevPeakA;

    float firstPeakEstimation;
    float history[];

	public AsgCounter()
	{
	    buffer = new float [BUFFER_SIZE];
	    Reset();
	}

	public void Reset()
	{
		config = new AsgCounterConfig();
		stats = new AsgStats();
		
	    warmup = true;
	    bufferPtr = 0;
	    state = AsgCounterState.BeforePeak;
	    samplePos = 0;
	    averageRMS = 0.0f;

	    reportsNum = 0;
	    prevPeakA = -1.0f;

	    peakSearchStart = new float [2];
	    history = new float [config.minPeakDistance + HISTORY_SAMPLES_BEFORE];

	    stats.Reset();
	}

	public AsgStats GetStats()
	{
	    return stats;
	}

	public AsgCounterConfig GetConfig()
	{
	    return config;
	}

	void ReportPeaksGroup(float peakA, float peakB)
	{
		System.out.format("#%d:\t A = %.2f,\t B = %.2f", reportsNum, peakA, peakB);

	    float velocity = -1.0f;
	    if (peakB > 0.0f)
	    {
	        float sampleDist = peakB - peakA;
	        velocity = config.length * config.sampleRate / sampleDist;
	        System.out.format(",\t d = %.2f,\t m/s = %.1f", sampleDist, velocity);
	    }
	    
	    System.out.println();

	    float dt = -1.0f;
	    if (prevPeakA > 0)
	        dt = (peakA - prevPeakA) / config.sampleRate;
	    stats.AddSample(velocity, dt);

	    prevPeakA = peakA;
	    reportsNum++;
	}

	float FindPeakInHistory()
	{
	    int maxID = 0;
	    float tmp = -1.0f;
	    for (int i = 0; i < config.minPeakDistance + HISTORY_SAMPLES_BEFORE; ++i)
	    {
	        float val = Math.abs(history[i]);
	        if (val > tmp)
	        {
	            tmp = val;
	            maxID = i;
	        }
	    }

	    if (maxID < HISTORY_SAMPLES_BEFORE || maxID >= config.minPeakDistance)
	        return (float)(maxID - HISTORY_SAMPLES_BEFORE);

	    // Hermite interpolation

	    int offset;
	    if (Math.abs(history[maxID - 1]) < Math.abs(history[maxID + 1]))
	        offset = -1;
	    else
	        offset = -2;

	    float y0 = history[maxID + offset];
	    float y1 = history[maxID + offset + 1];
	    float y2 = history[maxID + offset + 2];
	    float y3 = history[maxID + offset + 3];

	    float c0 = y1;
	    float c1 = 0.5f * (y2 - y0);
	    float c2 = y0 - 2.5f * y1 + 2.0f * y2 - 0.5f * y3;
	    float c3 = 1.5f * (y1 - y2) + 0.5f * (y3 - y0);

	    float dc0 = c1;
	    float dc1 = 2.0f * c2;
	    float dc2 = 3.0f * c3;

	    float delta = dc1 * dc1 - 4.0f * dc0 * dc2;

	    float x = 0.5f;
	    float y = 0.0f;
	    if (delta >= 0.0f)
	    {
	        float x0 = (-dc1 - (float)Math.sqrt(delta)) / (2.0f * dc2);
	        float x1 = (-dc1 + (float)Math.sqrt(delta)) / (2.0f * dc2);

	        float iy0 = ((c3 * x0 + c2) * x0 + c1) * x0 + c0;
	        float iy1 = ((c3 * x1 + c2) * x1 + c1) * x1 + c0;


	        if ((x0 < 0.0f || x0 > 1.0f) && (x1 < 0.0f || x1 > 1.0f))
	        {
	            // both maxima are beyond (0, 1) range
	            x = 0.5f;
	            y = 0.0f;
	        }
	        else if (x0 < 0.0f || x0 > 1.0f)
	        {
	            x = x1;
	            y = iy1;
	        }
	        else if (x1 < 0.0f || x1 > 1.0f)
	        {
	            x = x0;
	            y = iy0;
	        }
	        else if (Math.abs(iy0) > Math.abs(iy1))
	        {
	            x = x0;
	            y = iy0;
	        }
	        else
	        {
	            x = x1;
	            y = iy1;
	        }
	    }

	    // printf("  y0 = %6.3f, y1 = %6.3f, y2 = %6.3f, y3 = %6.3f  =>  y(%.2f) = %.2f\n", y0, y1, y2, y3, x, y);

	    return (float)(maxID + offset) + x;
	}

	boolean Analyze()
	{
	    // calculate buffer RMS
	    float rms = 0.0f;
	    for (int i = 0; i < BUFFER_SIZE; ++i)
	    {
	        float sample = buffer[i];
	        rms += sample * sample;
	    }
	    rms = (float) Math.sqrt(rms / (float)(BUFFER_SIZE));

	    
	    
	    // RMS smoothing
	    if (rms > averageRMS)
	        averageRMS = rms;
	    else
	        averageRMS += 0.5f * (rms - averageRMS);

	    if (warmup)
	    {
	        warmup = false;
	        samplePos += BUFFER_SIZE;
	        sampleInCurState += BUFFER_SIZE;
	        return false;
	    }

	    rms = averageRMS;
	    final float tresholdOffset = 0.001f;
	    float treshold = config.detectionSigma * rms + tresholdOffset;

		boolean peakFound = false;
	    for (int i = 0; i < BUFFER_SIZE; ++i)
	    {
	        float sample = buffer[i];
	        boolean above = (sample > treshold) || (-sample > treshold);

	        if (above && state == AsgCounterState.BeforePeak) // before any peak
	        {
	            peakSearchStart[0] = samplePos;
	            state = AsgCounterState.FirstPeak;
	            sampleInCurState = 0;

	            for (int j = 0; j < HISTORY_SAMPLES_BEFORE; ++j)
	            {
	                int id = (int)(i + j) - (int)HISTORY_SAMPLES_BEFORE;
	                history[j] = (id >= 0) ? buffer[id] : 0.0f;  // TODO
	            }
	            history[HISTORY_SAMPLES_BEFORE] = sample;
	        }
	        else if (state == AsgCounterState.FirstPeak)  // first peak
	        {
	            if (sampleInCurState >= config.minPeakDistance)
	            {
	                state = AsgCounterState.BetweenPeaks;
	                sampleInCurState = 0;
	            }
	            else
	                history[sampleInCurState + HISTORY_SAMPLES_BEFORE] = sample;
	        }
	        else if (state == AsgCounterState.BetweenPeaks)  // between peaks
	        {
	            if (above)
	            {
	                peakSearchStart[1] = samplePos;
	                state = AsgCounterState.SecondPeak;
	                sampleInCurState = 0;

	                firstPeakEstimation = peakSearchStart[0] + FindPeakInHistory();

	                // TODO
	                for (int j = 0; j < HISTORY_SAMPLES_BEFORE; ++j)
	                {
	                    int id = (int)(i + j) - (int)HISTORY_SAMPLES_BEFORE;
	                    history[j] = (id >= 0) ? buffer[id] : 0.0f;  // TODO
	                }
	                history[HISTORY_SAMPLES_BEFORE] = sample;
	            }
	            else if (sampleInCurState >= config.maxPeakDistance)
	            {
	                // distance between peaks is too large - ignore it
	                firstPeakEstimation = peakSearchStart[0] + FindPeakInHistory();
	                ReportPeaksGroup(firstPeakEstimation, -1.0f);
					peakFound = true;
	                state = AsgCounterState.BeforePeak;
	            }
	        }
	        else if (state == AsgCounterState.SecondPeak)  // second peak
	        {
	            if (sampleInCurState >= config.minPeakDistance)
	            {
	                float secondPeakEstimation = peakSearchStart[1] + FindPeakInHistory();
	                ReportPeaksGroup(firstPeakEstimation, secondPeakEstimation);
					peakFound = true;
	                state = AsgCounterState.BeforePeak;
	            }
	            else
	                history[sampleInCurState + HISTORY_SAMPLES_BEFORE] = sample;
	        }

	        samplePos++;
	        sampleInCurState++;
	    }

	    return peakFound;
	}

	boolean ProcessBuffer(float[] samples, int samplesNum)
	{
		boolean peakFound = false;

	    for (int i = 0; i < samplesNum; ++i)
	    {
	        buffer[bufferPtr++] = samples[i];
	        if (bufferPtr == BUFFER_SIZE)
	        {
				peakFound |= Analyze();
	            bufferPtr = 0;
	        }
	    }

		return peakFound;
	}
}
