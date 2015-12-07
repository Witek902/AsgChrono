package com.example.michal.asgchrono;

import java.util.ArrayList;
import java.util.List;

public class AsgStats {
	List<AsgStatsSample> history;

    // velocity in meters per second
    float velocityAvg, velocityMin, velocityMax, velocityStdDev;

    // fire rate in rounds per second
    float fireRateAvg, fireRateMin, fireRateMax, fireRateStdDev;
    
    public AsgStats()
    {
    	history = new ArrayList<AsgStatsSample>();
    	Reset();
    }
    
    public void Reset()
    {
        history.clear();
        fireRateMin = -1.0f;
        fireRateMax = -1.0f;
        fireRateAvg = -1.0f;
        fireRateStdDev = -1.0f;
    }

    public void Print()
    {
    	System.out.println("Stats (based on " + history.size() + " samples):");
    	System.out.format("Velocity:  avg = %.1f, min = %.1f, max = %.1f, std. dev. = %.2f%n",
               velocityAvg, velocityMin, velocityMax, velocityStdDev);
    	System.out.format("Fire rate: avg = %.2f, min = %.2f, max = %.2f, std. dev. = %.2f%n",
               fireRateAvg, fireRateMin, fireRateMax, fireRateStdDev);
    }

    public void Calc(AsgCounterConfig cfg)
    {
        velocityMin = Float.MAX_VALUE;
        velocityMax = Float.MIN_VALUE;
        velocityAvg = -1.0f;
        velocityStdDev = -1.0f;

        int validVelocitySamples = 0;
        float sum = 0.0f;

        // calculate average velocity
        for (int i = 0; i < history.size(); ++i)
        {
            if (history.get(i).velocity > 0.0f)
            {
                sum += history.get(i).velocity;
                if (history.get(i).velocity > velocityMax)
                    velocityMax = history.get(i).velocity;
                if (history.get(i).velocity < velocityMin)
                    velocityMin = history.get(i).velocity;
                validVelocitySamples++;
            }
        }

        if (validVelocitySamples > 0)
        {
            velocityAvg = sum / (float)validVelocitySamples;
            float stdDevSum = 0.0f;

            // calculate standard deviation
            for (int i = 0; i < history.size(); ++i)
            {
                if (history.get(i).velocity > 0.0f)
                {
                    float difference = velocityAvg - history.get(i).velocity;
                    stdDevSum += difference * difference;
                }
            }

            velocityStdDev = (float)Math.sqrt(stdDevSum / (float)validVelocitySamples);
        }

        // calculate fire rate stats
        if (history.size() > 2)
        {
            float lastDeltaTime = history.get(history.size() - 1).deltaTime;
            float minDeltaTime = lastDeltaTime;
            float maxDeltaTime = lastDeltaTime;
            float deltaTimeSum = lastDeltaTime;
            int samples = 1;

            for (int i = history.size() - 2; i > 0; --i)
            {
                float dt = history.get(i).deltaTime;

                if (dt > lastDeltaTime * cfg.fireRateTreshold)
                {
                    break;
                }

                // we are in semi-auto mode - do not calculate RoF
                if (dt * cfg.fireRateTreshold < lastDeltaTime)
                {
                    samples = 0;
                    break;
                }

                // TODO: support for minimum fire rate (config)

                if (dt > maxDeltaTime)
                    maxDeltaTime = dt;
                if (dt < minDeltaTime)
                    minDeltaTime = dt;
                deltaTimeSum += dt;
                samples++;
            }

            if (samples > 0)
            {
                fireRateMin = 1.0f / maxDeltaTime;
                fireRateMax = 1.0f / minDeltaTime;
                fireRateAvg = (float)samples / deltaTimeSum;
            }
            else
            {
                fireRateMin = -1.0f;
                fireRateMax = -1.0f;
                fireRateAvg = -1.0f;
                fireRateStdDev = -1.0f;
            }
        }
    }

    public void AddSample(float velocity, float deltaTime)
    {
        AsgStatsSample sample = new AsgStatsSample();
        sample.velocity = velocity;
        sample.deltaTime = deltaTime;
        history.add(sample);
    }
}
