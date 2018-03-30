package capstoneflows.rcdsmobile;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PreScanActivity extends AppCompatActivity {
    private Button prescanButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pre_scan);

        prescanButton = findViewById(R.id.pre_scan_button);
        prescanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
    }

    private void startScan(){
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}
