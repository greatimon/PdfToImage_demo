package com.example.jyn.pdftoimage_demo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    File root;
    AssetManager assetManager;
    Bitmap pageImage;
    TextView tv;
    PermissionListener permissionListener;
    private static final String TAG = "all_"+MainActivity.class.getSimpleName();
    Handler handler;
    int total_pages_num;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 퍼미션 리스너(테드_ 라이브러리)
        permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
//                Toast.makeText(a_profile.this, "권한 허가", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
//                Toast.makeText(a_profile.this, "권한 거부", Toast.LENGTH_SHORT).show();
            }
        };
        // 퍼미션 체크
        permission_check();

        handler = new Handler() {
            @SuppressLint("SetTextI18n")
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == 0) {
                    tv.setText("Successfully rendered image_ count: " + String.valueOf(total_pages_num));
                    total_pages_num = 0;
                }
            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        setup();
    }
    /**
     * Initializes variables used for convenience
     */
    private void setup() {
        // Enable Android-style asset loading (highly recommended)
        PDFBoxResourceLoader.init(getApplicationContext());
        // Find the root of the external storage.
        root = android.os.Environment.getExternalStorageDirectory();
        assetManager = getAssets();
        tv = (TextView) findViewById(R.id.statusTextView);
    }

    private void permission_check() {
        new TedPermission(this)
                .setPermissionListener(permissionListener)
//                .setRationaleMessage("다음 작업을 허용하시겠습니까? 기기 사진, 미디어, 파일 액세스")
                .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다")
                .setGotoSettingButton(true)
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    /**
     * Creates a new PDF from scratch and saves it to a file
     */
    public void createPdf(View view) {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);
        Log.d(TAG, "document.getNumberOfPages(): " + document.getNumberOfPages());

        // Create a new font object selecting one of the PDF base fonts
        PDFont font = PDType1Font.HELVETICA;
        // Or a custom font
//		try {
//			PDType0Font font = PDType0Font.load(document, assetManager.open("MyFontFile.TTF"));
//		} catch(IOException e) {
//			e.printStackTrace();
//		}

        PDPageContentStream contentStream;

        try {
            // Define a content stream for adding to the PDF
            contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();

            // Load in the images
            InputStream in = assetManager.open("falcon.jpg");
            InputStream alpha = assetManager.open("trans.png");

            // Draw a green rectangle
            contentStream.addRect(5, 500, 100, 100);
            contentStream.setNonStrokingColor(0, 255, 125);
            contentStream.fill();

            // Draw the falcon base image
            PDImageXObject ximage = JPEGFactory.createFromStream(document, in);
            contentStream.drawImage(ximage, 20, 20);

            // Draw the red overlay image
            Bitmap alphaImage = BitmapFactory.decodeStream(alpha);
            PDImageXObject alphaXimage = LosslessFactory.createFromImage(document, alphaImage);
            contentStream.drawImage(alphaXimage, 20, 20 );

            // Make sure that the content stream is closed:
            contentStream.close();

            // Save the final pdf document to a file
            String path = root.getAbsolutePath() + "/Download/Created.pdf";
            document.save(path);
            document.close();
            tv.setText("Successfully wrote PDF to " + path);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Loads an existing PDF and renders it to a Bitmap
     */
    @SuppressLint("SetTextI18n")
    public void renderFile(View view) {
        // Render the page and save it to an image file
        try {
            // Load in an already created PDF
            final PDDocument document = PDDocument.load(assetManager.open("test5.pdf"));
            // Create a renderer for the document
            final PDFRenderer renderer = new PDFRenderer(document);
            Log.d(TAG, "PDF 파일 총 페이지 수: " + document.getNumberOfPages());
            total_pages_num = document.getNumberOfPages();

            new Thread() {
                @Override
                public void run() {
                    super.run();

                    try {
                        for(int i=1; i<=document.getNumberOfPages(); i++) {
                            // Render the image to an RGB Bitmap
                            pageImage = renderer.renderImage(i-1, 1, Bitmap.Config.RGB_565);
                            // Save the render result to an image
                            String path = root.getAbsolutePath() + "/Download/test5_" + String.valueOf(i) + ".png";
                            File renderFile = new File(path);
                            FileOutputStream fileOut = new FileOutputStream(renderFile);
                            pageImage.compress(Bitmap.CompressFormat.PNG, 100, fileOut);
                            fileOut.close();

                            if(i==document.getNumberOfPages()) {
                                handler.sendEmptyMessage(0);
                            }

                            Log.d(TAG, "test5_" + String.valueOf(i) + ".jpg" + "변환 완료");
                        }
                    }
                    catch(Exception e) {
                        e.printStackTrace();
                    }

                }
            }.start();

//            for(int i=1; i<=document.getNumberOfPages(); i++) {
//                // Render the image to an RGB Bitmap
//                pageImage = renderer.renderImage(i-1, 1, Bitmap.Config.RGB_565);
//                // Save the render result to an image
//                String path = root.getAbsolutePath() + "/Download/test5_" + String.valueOf(i) + ".jpg";
//                File renderFile = new File(path);
//                FileOutputStream fileOut = new FileOutputStream(renderFile);
//                pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
//                fileOut.close();
//
//                if(i==document.getNumberOfPages()) {
//                    tv.setText("Successfully rendered image_ count: " + String.valueOf(document.getNumberOfPages()));
//                }
//            }
//            // Render the image to an RGB Bitmap
//            pageImage = renderer.renderImage(0, 1, Bitmap.Config.RGB_565);
//
//            // Save the render result to an image
//            String path = root.getAbsolutePath() + "/Download/render.jpg";
//            File renderFile = new File(path);
//            FileOutputStream fileOut = new FileOutputStream(renderFile);
//            pageImage.compress(Bitmap.CompressFormat.JPEG, 100, fileOut);
//            fileOut.close();
//            tv.setText("Successfully rendered image to " + path);
//            // Optional: display the render result on screen
//            displayRenderedImage();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fills in a PDF form and saves the result
     */
    public void fillForm(View view) {
//        try {
//            // Load the document and get the AcroForm
//            PDDocument document = PDDocument.load(assetManager.open("FormTest.pdf"));
//            PDDocumentCatalog docCatalog = document.getDocumentCatalog();
//            PDAcroForm acroForm = docCatalog.getAcroForm();
//
//            // Fill the text field
//            PDTextField field = (PDTextField) acroForm.getField("TextField");
//            field.setValue("Filled Text Field");
//            // Optional: don't allow this field to be edited
//            field.setReadOnly(true);
//
//            PDField checkbox = acroForm.getField("Checkbox");
//            ((PDCheckbox) checkbox).check();
//
//            PDField radio = acroForm.getField("Radio");
//            ((PDRadioButton)radio).setValue("Second");
//
//            PDField listbox = acroForm.getField("ListBox");
//            List<Integer> listValues = new ArrayList<>();
//            listValues.add(1);
//            listValues.add(2);
//            ((PDListBox) listbox).setSelectedOptionsIndex(listValues);
//
//            PDField dropdown = acroForm.getField("Dropdown");
//            ((PDComboBox) dropdown).setValue("Hello");
//
//            String path = root.getAbsolutePath() + "/Download/FilledForm.pdf";
//            tv.setText("Saved filled form to " + path);
//            document.save(path);
//            document.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void stripText(View view) {
        String parsedText = null;
        PDDocument document = null;
        try {
            document = PDDocument.load(assetManager.open("Hello.pdf"));
        } catch(IOException e) {
            e.printStackTrace();
        }

        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            pdfStripper.setStartPage(0);
            pdfStripper.setEndPage(1);
            parsedText = "Parsed text: " + pdfStripper.getText(document);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (document != null) document.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        tv.setText(parsedText);
    }

    public void createEncryptedPdf()
    {
        String path = root.getAbsolutePath() + "/Download/crypt.pdf";

        int keyLength = 128; // 128 bit is the highest currently supported

        // Limit permissions of those without the password
        AccessPermission ap = new AccessPermission();
        ap.setCanPrint(false);

        // Sets the owner password and user password
        StandardProtectionPolicy spp = new StandardProtectionPolicy("12345", "hi", ap);

        // Setups up the encryption parameters
        spp.setEncryptionKeyLength(keyLength);
        spp.setPermissions(ap);
        BouncyCastleProvider provider = new BouncyCastleProvider();
        Security.addProvider(provider);

        PDFont font = PDType1Font.HELVETICA;
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();

        document.addPage(page);

        try
        {
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            // Write Hello World in blue text
            contentStream.beginText();
            contentStream.setNonStrokingColor(15, 38, 192);
            contentStream.setFont(font, 12);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("Hello World");
            contentStream.endText();
            contentStream.close();

            // Save the final pdf document to a file
            document.protect(spp); // Apply the protections to the PDF
            document.save(path);
            document.close();

        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Helper method for drawing the result of renderFile() on screen
     */
    private void displayRenderedImage() {
        new Thread() {
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ImageView imageView = (ImageView) findViewById(R.id.renderedImageView);
                        imageView.setImageBitmap(pageImage);
                    }
                });
            }
        }.start();
    }
}
