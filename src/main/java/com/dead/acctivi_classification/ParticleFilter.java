package com.dead.acctivi_classification;

import android.graphics.PointF;

import java.util.Random;

public class ParticleFilter {
    static Particle[] particles;

    int numParticles = 0;
    public int eliminateCount = 0;
    private float xScale = 2613/48.99f;
    private float yScale = 810/15.04f;
    Random gen = new Random();

    public ParticleFilter(int numParticles, PointF[] landmarks, int width, int height){

        this.numParticles = numParticles;

        particles = new Particle[numParticles];
        for (int i = 0; i < numParticles; i++) {
            particles[i] = new Particle(landmarks, width, height);
        }
        // generateInitialParticles//
    }


    public void move(float turn, float forward) throws Exception {
        for (int i = 0; i < numParticles; i++) {
            particles[i].move(turn, forward);
        }
    }

    public void resample(float[] measurement) throws Exception {//the new distances are sent.
        Particle[] new_particles = new Particle[numParticles];

        for (int i = 0; i < numParticles; i++) {
            particles[i].measurementProb(measurement); //calculate distance wrt landmarks and obtain new probability
        }
        float B = 0f;
        Particle best = getBestParticle();//process of finding the next best particle
        int index = (int) gen.nextFloat() * numParticles;
        for (int i = 0; i < numParticles; i++) {
            B += gen.nextFloat() * 2f * best.probability;
            while (B > particles[index].probability) {
                B -= particles[index].probability;
                index = circle(index + 1, numParticles);
            }//not sure of the noises. but they are used as SD in calculation of gausian distance
            new_particles[i] = new Particle(particles[index].landmarks, particles[index].worldWidth, particles[index].worldHeight);
            new_particles[i].set(particles[index].x, particles[index].y, particles[index].orientation, particles[index].probability);
            new_particles[i].setNoise(particles[index].forwardNoise, particles[index].turnNoise, particles[index].senseNoise);
        }

        particles = new_particles;//new set of particles are generated
    }


    void calNewProbability(Particle[] par){
        double totalWeight = 0;
        for (int i = 0; i < par.length; i++ ){
            totalWeight += par[i].probability ;
        }
        for (int i = 0; i < par.length; i++ ){
            par[i].probability = par[i].probability / totalWeight ;
        }
    }


    void reSamp(Particle[] par) throws Exception {
        calNewProbability(par);
        Particle[] new_particles = new Particle[numParticles];
        int x = 0;
        double add = 0;
        for (int i = 0; i < par.length; i++ ){
            int y = (int) Math.round(par[i].probability * Double.valueOf(eliminateCount));
            if (y == 0){ add = par[i].probability * Double.valueOf(eliminateCount); }
            if (y > 0){
                Double xx = Double.valueOf(y) + add;
                y = Integer.valueOf(xx.toString());
                for (int ii = 0; ii < y ; ii++ ) {
                    new_particles[x + ii].set(par[i].x, par[i].y, par[i].orientation, par[i].probability);

                }
                x += y;
                new_particles[x].set(par[i].x, par[i].y, par[i].orientation, par[i].probability);
                x += 1;
                add = 0;
            }
        }
        //Particle best = getBestParticle();
        particles = new_particles;//new set of particles are generated
    }

    void eliminate() throws Exception {
        int count= 0;

        for (int i = 0; i < numParticles; i++) {

            if (particles[i].x > 2.3f * xScale && particles[i].x <= 9.09f * xScale){
                if (particles[i].y <= 9.0f * yScale || particles[i].y >= 14.04f * yScale  ){
                    particles[i].set(particles[i].x, particles[i].y, particles[i].orientation,0.0);
                    count += 1;
                }
            }
            if (particles[i].x > 9.09f * xScale && particles[i].x <= 16.84f* xScale ){
                if (particles[i].y <= 9.0f * yScale || particles[i].y >= 10.65f * yScale ){
                    particles[i].set(particles[i].x, particles[i].y, particles[i].orientation, 0);
                    count += 1;
                }
            }
            if (particles[i].x > 16.84f* xScale && particles[i].x <= 19.14f* xScale ){
                if (particles[i].y <= 6.18f * yScale || particles[i].y >= 13.44f * yScale ){
                    particles[i].set(particles[i].x, particles[i].y, particles[i].orientation, 0);
                    count += 1;
                }}
            if (particles[i].x > 19.14f * xScale& particles[i].x <= 31.43f* xScale ){
                if (particles[i].y <= 9.0f * yScale || particles[i].y >= 10.65f * yScale  ){
                    particles[i].set(particles[i].x, particles[i].y, particles[i].orientation, 0);
                    count += 1;
                }
            }

            if (particles[i].x > 31.43f * xScale || particles[i].x <= 2.3f * xScale ){
                    particles[i].set(particles[i].x, particles[i].y, particles[i].orientation, 0);
                count += 1;
            }
        }
        eliminateCount = count;

    }

    public Particle getBestParticle() {
        Particle particle = particles[0];
        for (int i = 1; i < numParticles; i++) {
            if (particles[i].probability > particle.probability) {
                particle = particles[i];
            }
        }
        return particle;
    }

    private int circle(int i, int numParticles) {
        while (i>numParticles - 1) {
            i-= numParticles;
        }
        while (i < 0) {
            i += numParticles;
        }
        return i;
    }


}