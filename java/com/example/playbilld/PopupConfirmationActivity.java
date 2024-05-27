package com.example.playbilld;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**egy kvázi sablonos popup activity, ami tetszőleges szöveggel
 * és gombfeliratokkal ruházható fel**/
public class PopupConfirmationActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_popup_confirmation);

        Intent intent = getIntent();
        String message = intent.getStringExtra("message");
        String no = intent.getStringExtra("no");
        String yes = intent.getStringExtra("yes");

        Button buttonNo = findViewById(R.id.buttonNo);
        Button buttonYes = findViewById(R.id.buttonYes);
        TextView messageView = findViewById(R.id.textViewMessage);

        messageView.setText(message);
        buttonNo.setText(no);
        buttonYes.setText(yes);

        buttonNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancel();
            }
        });

        buttonYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm();
            }
        });

    }

    private void cancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void confirm() {
        setResult(RESULT_OK);
        finish();
    }
}