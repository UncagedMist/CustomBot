package kk.techbytecare.testchatbot;

import android.Manifest;
import android.content.res.AssetManager;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.alicebot.ab.AIMLProcessor;
import org.alicebot.ab.Bot;
import org.alicebot.ab.Chat;
import org.alicebot.ab.MagicStrings;
import org.alicebot.ab.PCAIMLProcessorExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import kk.techbytecare.testchatbot.Adapter.ChatMessageAdapter;
import kk.techbytecare.testchatbot.Model.ChatMessage;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    FloatingActionButton btnSend;
    EditText edtTextMsg;
    ImageView imageView;

    public Bot bot;
    public static Chat chat;
    private ChatMessageAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnSend = findViewById(R.id.btnSend);
        edtTextMsg = findViewById(R.id.edtTextMsg);
        imageView = findViewById(R.id.imageView);

        adapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        listView.setAdapter(adapter);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String message = edtTextMsg.getText().toString();

                String response = chat.multisentenceRespond(edtTextMsg.getText().toString());
                if (TextUtils.isEmpty(message)) {
                    Toast.makeText(MainActivity.this, "Please enter a query..", Toast.LENGTH_SHORT).show();
                    return;
                }
                sendMessage(message);
                botsReply(response);
                edtTextMsg.setText("");
                listView.setSelection(adapter.getCount() - 1);
            }
        });

        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                ).withListener(new MultiplePermissionsListener() {
                                   @Override
                                   public void onPermissionsChecked(MultiplePermissionsReport report) {

                                       if (report.areAllPermissionsGranted())  {

                                           boolean available = isSDCARDAvailable();

                                           AssetManager assets = getResources().getAssets();
                                           File fileName = new File(Environment.getExternalStorageDirectory().toString() + "/TBC/bots/bot");

                                           boolean makeFile = fileName.mkdirs();

                                           if (fileName.exists()) {
                                               //Reading the file
                                               try {
                                                   for (String dir : assets.list("bot")) {

                                                       File subDir = new File(fileName.getPath() + "/" + dir);
                                                       boolean subDir_check = subDir.mkdirs();

                                                       for (String file : assets.list("bot/" + dir)) {
                                                           File newFile = new File(fileName.getPath() + "/" + dir + "/" + file);
                                                           if (newFile.exists()) {
                                                               continue;
                                                           }
                                                           InputStream in;
                                                           OutputStream out;

                                                           in = assets.open("bot/" + dir + "/" + file);
                                                           out = new FileOutputStream(fileName.getPath() + "/" + dir + "/" + file);

                                                           //copy file from assets to the mobile's SD card or any secondary memory
                                                           copyFile(in, out);
                                                           in.close();
                                                           out.flush();
                                                           out.close();
                                                       }
                                                   }
                                               } catch (IOException e) {
                                                   e.printStackTrace();
                                               }
                                           }

                                           //get the working directory
                                           MagicStrings.root_path = Environment.getExternalStorageDirectory().toString() + "/TBC";
                                           AIMLProcessor.extension =  new PCAIMLProcessorExtension();

                                           bot = new Bot("bot", MagicStrings.root_path, "chat");
                                           chat = new Chat(bot);


                                           Toast.makeText(MainActivity.this, "Permission Granted.", Toast.LENGTH_SHORT).show();
                                       }
                                       if (report.isAnyPermissionPermanentlyDenied())  {
                                           Toast.makeText(MainActivity.this, "Please Grant all permission...", Toast.LENGTH_SHORT).show();
                                       }
                                   }

                                   @Override
                                   public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                                       token.continuePermissionRequest();
                                   }
                               }
        ).withErrorListener(new PermissionRequestErrorListener() {
            @Override
            public void onError(DexterError error) {
                Toast.makeText(MainActivity.this, "Error Occurred..", Toast.LENGTH_SHORT).show();
            }
        }).onSameThread().check();
    }

    private void sendMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true, false);
        adapter.add(chatMessage);
    }

    private void botsReply(String message) {
        ChatMessage chatMessage = new ChatMessage(message, false, false);
        adapter.add(chatMessage);
    }


    public static boolean isSDCARDAvailable(){
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)? true :false;
    }

    //copying the file
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
