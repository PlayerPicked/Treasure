package com.playerpicked.treasure.particle;

import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum ParticleShape {
    HELIX {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double angle = tick * 0.1;
            for (double y = 0; y <= HEIGHT; y += HEIGHT / DENSITY) {
                double twist = angle + y * 2 * Math.PI / HEIGHT;
                points.add(new Vector(RADIUS * Math.cos(twist), y, RADIUS * Math.sin(twist)));
                points.add(new Vector(RADIUS * Math.cos(twist + Math.PI), y, RADIUS * Math.sin(twist + Math.PI)));
            }
        }
    },
    HEART {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double scale = RADIUS * ((tick * 0.005) % 1.0);
            for (double t = 0; t <= Math.PI * 2; t += Math.PI / 16) {
                double x = scale * Math.pow(Math.sin(t), 3);
                double y = scale * (13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 16;
                points.add(new Vector(x, y + 1, 0));
            }
        }
    },
    PULSAR {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double pulseRadius = pingPong(tick * 0.01, RADIUS);
            for (int i = 0; i < DENSITY; i++) {
                double angle = i * 2 * Math.PI / DENSITY;
                double x = pulseRadius * Math.cos(angle);
                double z = pulseRadius * Math.sin(angle);
                points.add(new Vector(x, 0, z));
                points.add(new Vector(x, pulseRadius * Math.sin(angle) * 0.5, z));
            }
        }
    },
    SPHERE {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double rotationX = tick * 0.05;
            double rotationY = tick * 0.03;
            int rings = DENSITY / 2;

            for (int i = 0; i < rings; i++) {
                double phi = Math.acos(-1 + 2.0 * i / rings);
                for (int j = 0; j < rings; j++) {
                    double theta = 2 * Math.PI * j / rings;
                    double x = RADIUS * Math.sin(phi) * Math.cos(theta);
                    double y = RADIUS * Math.sin(phi) * Math.sin(theta);
                    double z = RADIUS * Math.cos(phi);

                    double turnedX = x * Math.cos(rotationX) - z * Math.sin(rotationX);
                    double turnedZ = x * Math.sin(rotationX) + z * Math.cos(rotationX);
                    double turnedY = y * Math.cos(rotationY) - turnedZ * Math.sin(rotationY);
                    turnedZ = y * Math.sin(rotationY) + turnedZ * Math.cos(rotationY);

                    points.add(new Vector(turnedX, turnedY + 1, turnedZ));
                }
            }
        }
    },
    TORNADO {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double angle = tick * 0.2;
            double lift = pingPong(tick * 0.1, HEIGHT);

            for (double y = 0; y <= HEIGHT; y += HEIGHT / DENSITY) {
                double radiusAtHeight = RADIUS * (1 - y / HEIGHT);
                for (int i = 0; i < 8; i++) {
                    double circleAngle = angle + i * Math.PI / 4;
                    points.add(new Vector(radiusAtHeight * Math.cos(circleAngle), (y + lift) % HEIGHT, radiusAtHeight * Math.sin(circleAngle)));
                }
            }
        }
    },
    PHOENIX {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double time = (tick * 0.1) % (2 * Math.PI);
            double flap = Math.sin(time * 0.3) * 0.8;
            double wingAngle = Math.sin(time * 0.1) * Math.PI / 6;

            for (int side = -1; side <= 1; side += 2) {
                for (int i = 0; i <= DENSITY; i++) {
                    double t = (double) i / DENSITY;
                    double wingShape = Math.sin(t * 3) * (1 - t * t);
                    double x = side * t * RADIUS * Math.cos(wingAngle);
                    double y = wingShape * HEIGHT + flap * Math.sin(t * Math.PI);
                    double z = t * RADIUS * Math.sin(wingAngle) + wingShape * 0.3;
                    points.add(new Vector(x, y, z));

                    if (t <= 0.3) continue;
                    for (int feather = -1; feather <= 1; feather++) {
                        double offset = feather * 0.2;
                        points.add(new Vector(x + side * offset * Math.cos(wingAngle + Math.PI / 2), y, z + offset * Math.sin(wingAngle + Math.PI / 2)));
                    }
                }
            }
            points.add(new Vector(0, HEIGHT * 0.3, 0));
        }
    },
    SPIRAL {
        @Override
        void addPoints(long tick, @NotNull List<Vector> points) {
            double angle = tick * 0.02;

            for (int arm = 0; arm < 3; arm++) {
                double armAngle = angle + arm * 2 * Math.PI / 3;
                for (int i = 0; i < DENSITY; i++) {
                    double distance = (double) i / DENSITY;
                    double spiralAngle = armAngle + distance * 2 * Math.PI;
                    double spiralRadius = distance * RADIUS;
                    points.add(new Vector(spiralRadius * Math.cos(spiralAngle), Math.sin(distance * Math.PI * 2) * 0.5, spiralRadius * Math.sin(spiralAngle)));
                }
            }

            for (int i = 0; i < 5; i++) {
                double centerAngle = angle * 2 + i * 2 * Math.PI / 5;
                points.add(new Vector(RADIUS * 0.1 * Math.cos(centerAngle), 0, RADIUS * 0.1 * Math.sin(centerAngle)));
            }
        }
    };

    private static final double RADIUS = 1.0;
    private static final double HEIGHT = 2.0;
    private static final int DENSITY = 10;

    abstract void addPoints(long tick, @NotNull List<Vector> points);

    public @NotNull List<Vector> pointsAt(long tick) {
        List<Vector> points = new ArrayList<>();
        addPoints(tick, points);
        return points;
    }

    public @NotNull ParticleShape next() {
        ParticleShape[] shapes = values();
        return shapes[(ordinal() + 1) % shapes.length];
    }

    public @NotNull ParticleShape previous() {
        ParticleShape[] shapes = values();
        return shapes[(ordinal() - 1 + shapes.length) % shapes.length];
    }

    public static @NotNull Optional<ParticleShape> byName(@NotNull String name) {
        try {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static double pingPong(double value, double max) {
        double phase = value % (2 * max);
        return phase <= max ? phase : 2 * max - phase;
    }
}
