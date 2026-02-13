package com.example.detectarostrosml;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    TextView txtResults;
    ImageView mImageView;
    Bitmap mSelectedImage;


    public static int REQUEST_CAMERA = 111;
    public static int REQUEST_GALLERY = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtResults = findViewById(R.id.txtresults);
        mImageView = findViewById(R.id.image_view);
        txtResults.setMovementMethod(new android.text.method.ScrollingMovementMethod());

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
        }
    }

    public void abrirGaleria(View view) {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, REQUEST_GALLERY);
    }

    public void abrirCamera(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            try {
                if (requestCode == REQUEST_CAMERA) {
                    mSelectedImage = (Bitmap) data.getExtras().get("data");
                } else {
                    mSelectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                }
                mImageView.setImageBitmap(mSelectedImage);
                txtResults.setText("Imagen cargada. Elija una función abajo.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void OCRfx(View v) {
        if (mSelectedImage == null) {
            txtResults.setText("Por favor, seleccione una imagen primero");
            return;
        }

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(new OnSuccessListener<Text>() {
                    @Override
                    public void onSuccess(Text text) {
                        String resultados = "";
                        List<Text.TextBlock> blocks = text.getTextBlocks();
                        if (blocks.size() == 0) {
                            resultados = "No hay Texto en la imagen";
                        } else {
                            for (Text.TextBlock block : blocks) {
                                resultados += block.getText() + "\n";
                            }
                        }
                        txtResults.setText(resultados);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResults.setText("Error al procesar texto");
                    }
                });
    }

    public void Rostrosfx(View v) {
        if (mSelectedImage == null) return;

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);

        FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                .build();

        FaceDetector detector = FaceDetection.getClient(options);

        detector.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @Override
                    public void onSuccess(List<Face> faces) {
                        if (faces.size() == 0) {
                            txtResults.setText("No hay rostros detectados");
                        } else {
                            txtResults.setText("Hay " + faces.size() + " rostro(s)");
                            pintaRecuadros(faces); // Llama a la función de dibujo
                        }
                    }
                });
    }

    private void pintaRecuadros(List<Face> faces) {
        Bitmap bitmap = mSelectedImage.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(8f);
        paint.setStyle(Paint.Style.STROKE);

        for (Face face : faces) {
            canvas.drawRect(face.getBoundingBox(), paint);
        }
        mImageView.setImageBitmap(bitmap);
    }

    public void Labeling(View v) {
        if (mSelectedImage == null) return;

        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);

        labeler.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<ImageLabel>>() {
                    @Override
                    public void onSuccess(List<ImageLabel> labels) {
                        String resultados = "Objetos encontrados:\n";
                        for (ImageLabel label : labels) {
                            resultados += label.getText() + " (" + (int)(label.getConfidence() * 100) + "%)\n";
                        }
                        txtResults.setText(resultados);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        txtResults.setText("Error en el etiquetado");
                    }
                });
    }
}