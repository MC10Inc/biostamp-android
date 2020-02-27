package com.mc10inc.biostamp3.sdkexample;

import android.os.AsyncTask;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.RecordingAnnotation;
import com.mc10inc.biostamp3.sdk.sensing.RecordingDecoder;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import timber.log.Timber;

public class DecodeRecordingTask extends AsyncTask<Void, Void, Void> {
    private RecordingInfo recordingInfo;
    private OutputStream outputStream;

    public DecodeRecordingTask(RecordingInfo recordingInfo, OutputStream outputStream) {
        this.recordingInfo = recordingInfo;
        this.outputStream = outputStream;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try (ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream((outputStream)))) {
            if (recordingInfo.getSensorConfig().hasMotion()) {
                decodeMotion(zip);
            }
            if (recordingInfo.getSensorConfig().hasAd5940()) {
                decodeAd5940(zip);
            }
            if (recordingInfo.getSensorConfig().hasAfe4900()) {
                decodeAfe4900(zip);
            }
            if (recordingInfo.getSensorConfig().hasEnvironment()) {
                decodeEnvironment(zip);
            }
            decodeAnnotations(zip);
        } catch (IOException e) {
            Timber.e(e);
        }
        return null;
    }

    private void decodeAd5940(ZipOutputStream zip) throws IOException {
        Timber.i("Writing ad5940.csv");
        ZipEntry entry = new ZipEntry("ad5940.csv");
        zip.putNextEntry(entry);
        OutputStream bos = new BufferedOutputStream(zip);
        bos.write("timestamp,z_mag,z_phase\n".getBytes(StandardCharsets.US_ASCII));

        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);

        decoder.setListener(RecordingDecoder.RawSamplesType.AD5940, samples -> {
            try {
                for (int i = 0; i < samples.getSize(); i++) {
                    NumberWriter.write(samples.getTimestamp(i), 3, bos);
                    bos.write(',');
                    NumberWriter.write(samples.getValue(RawSamples.ColumnType.Z_MAG, i), 0, bos);
                    bos.write(',');
                    NumberWriter.write(samples.getValue(RawSamples.ColumnType.Z_PHASE, i), 6, bos);
                    bos.write('\n');
                }
            } catch (IOException e) {
                Timber.e(e);
            }
        });
        decoder.decode();
        bos.flush();
        zip.closeEntry();
    }

    private void decodeAfe4900(ZipOutputStream zip) throws IOException {
        Timber.i("Writing afe4900.csv");
        ZipEntry entry = new ZipEntry("afe4900.csv");
        zip.putNextEntry(entry);
        OutputStream bos = new BufferedOutputStream(zip);

        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);
        boolean ecg = recordingInfo.getSensorConfig().hasAfe4900Ecg();
        boolean ppg = recordingInfo.getSensorConfig().hasAfe4900Ppg();
        if (ecg && ppg) {
            bos.write("timestamp,ecg_uv,ppg\n".getBytes(StandardCharsets.US_ASCII));
        } else if (ecg) {
            bos.write("timestamp,ecg_uv\n".getBytes(StandardCharsets.US_ASCII));
        } else if (ppg) {
            bos.write("timestamp,ppg\n".getBytes(StandardCharsets.US_ASCII));
        } else {
            Timber.e("Unsupported mode");
            zip.closeEntry();
            return;
        }
        decoder.setListener(RecordingDecoder.RawSamplesType.AFE4900, samples -> {
            try {
                for (int i = 0; i < samples.getSize(); i++) {
                    NumberWriter.write(samples.getTimestamp(i), 3, bos);
                    if (ecg) {
                        bos.write(',');
                        NumberWriter.write(samples.getValue(RawSamples.ColumnType.ECG, i) * 1000000, 6, bos);
                    }
                    if (ppg) {
                        bos.write(',');
                        NumberWriter.write(samples.getValue(RawSamples.ColumnType.PPG, i), 0, bos);
                    }
                    bos.write('\n');
                }
            } catch (IOException e) {
                Timber.e(e);
            }
        });
        decoder.decode();
        bos.flush();
        zip.closeEntry();
    }

    private void decodeEnvironment(ZipOutputStream zip) throws IOException {
        Timber.i("Writing environment.csv");
        ZipEntry entry = new ZipEntry("environment.csv");
        zip.putNextEntry(entry);
        OutputStream bos = new BufferedOutputStream(zip);

        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);
        bos.write("pascals,temperature_c,external_temperature_c\n".getBytes(StandardCharsets.US_ASCII));
        decoder.setListener(RecordingDecoder.RawSamplesType.ENVIRONMENT, samples -> {
            try {
                for (int i = 0; i < samples.getSize(); i++) {
                    NumberWriter.write(samples.getTimestamp(i), 3, bos);
                    bos.write(',');
                    NumberWriter.write(samples.getValue(RawSamples.ColumnType.PASCALS, i), 0, bos);
                    bos.write(',');
                    NumberWriter.write(samples.getValue(RawSamples.ColumnType.TEMPERATURE, i), 0, bos);
                    bos.write(',');
                    NumberWriter.write(samples.getValue(RawSamples.ColumnType.EXTERNAL_TEMPERATURE, i), 3, bos);
                    bos.write('\n');
                }
            } catch (IOException e) {
                Timber.e(e);
            }
        });
        decoder.decode();
        bos.flush();
        zip.closeEntry();
    }

    private void decodeMotion(ZipOutputStream zip) throws IOException {
        Timber.i("Writing motion.csv");
        ZipEntry entry = new ZipEntry("motion.csv");
        zip.putNextEntry(entry);
        OutputStream bos = new BufferedOutputStream(zip);

        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);
        final RawSamples.ColumnType[] cols;
        if (recordingInfo.getSensorConfig().hasMotionAccel()) {
            if (recordingInfo.getSensorConfig().hasMotionGyro()) {
                cols = new RawSamples.ColumnType[]{
                        RawSamples.ColumnType.ACCEL_X,
                        RawSamples.ColumnType.ACCEL_Y,
                        RawSamples.ColumnType.ACCEL_Z,
                        RawSamples.ColumnType.GYRO_X,
                        RawSamples.ColumnType.GYRO_Y,
                        RawSamples.ColumnType.GYRO_Z
                };
                bos.write("timestamp,accel_x,accel_y,accel_z,gyro_x,gyro_y,gyro_z\n"
                        .getBytes(StandardCharsets.US_ASCII));
            } else {
                cols = new RawSamples.ColumnType[]{
                        RawSamples.ColumnType.ACCEL_X,
                        RawSamples.ColumnType.ACCEL_Y,
                        RawSamples.ColumnType.ACCEL_Z
                };
                bos.write("timestamp,accel_x,accel_y,accel_z".getBytes(StandardCharsets.US_ASCII));
            }
        } else {
            Timber.e("Unsupported mode");
            zip.closeEntry();
            return;
        }
        decoder.setListener(RecordingDecoder.RawSamplesType.MOTION, samples -> {
            try {
                for (int i = 0; i < samples.getSize(); i++) {
                    NumberWriter.write(samples.getTimestamp(i), 3, bos);
                    bos.write(',');
                    for (int j = 0; j < cols.length; j++) {
                        NumberWriter.write(samples.getValue(cols[j], i), 6, bos);
                        if (j == cols.length - 1) {
                            bos.write('\n');
                        } else {
                            bos.write(',');
                        }
                    }
                }
            } catch (IOException e) {
                Timber.e(e);
            }
        });
        decoder.decode();
        bos.flush();
        zip.closeEntry();
    }

    private void decodeAnnotations(ZipOutputStream zip) throws IOException {
        List<RecordingAnnotation> annotations = new ArrayList<>();
        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);
        decoder.setAnnotationListener(annotations::add);
        decoder.decode();
        if (annotations.isEmpty()) {
            return;
        }

        Timber.i("Writing annotations.csv");
        ZipEntry entry = new ZipEntry("annotations.csv");
        zip.putNextEntry(entry);
        OutputStream bos = new BufferedOutputStream(zip);
        PrintWriter pw = new PrintWriter(bos);

        pw.println("timestamp,annotation");
        for (RecordingAnnotation annotation : annotations) {
            pw.format("%.3f,%s\n", annotation.getTimestamp(),
                    new String(annotation.getData(), StandardCharsets.UTF_8));
        }

        pw.flush();
        bos.flush();
        zip.closeEntry();
    }
}
