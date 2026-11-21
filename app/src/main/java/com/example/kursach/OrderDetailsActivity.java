package com.example.kursach;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OrderDetailsActivity extends AppCompatActivity {
    public static final String EXTRA_DATE = "order_date";
    public static final String EXTRA_TITLE = "order_title";
    public static final String EXTRA_DESCRIPTION = "order_description";
    public static final String EXTRA_CITY = "order_city";
    public static final String EXTRA_WORK_MODE = "order_work_mode";
    public static final String EXTRA_PAYOUT = "order_payout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_details);

        findViewById(R.id.backButton).setOnClickListener(view -> finish());
        ((TextView) findViewById(R.id.orderDate)).setText(
                getIntent().getStringExtra(EXTRA_DATE));
        ((TextView) findViewById(R.id.orderTitle)).setText(
                getIntent().getStringExtra(EXTRA_TITLE));
        ((TextView) findViewById(R.id.orderDescription)).setText(
                getIntent().getStringExtra(EXTRA_DESCRIPTION));
        ((TextView) findViewById(R.id.orderCity)).setText(
                getIntent().getStringExtra(EXTRA_CITY));
        ((TextView) findViewById(R.id.orderPayout)).setText(
                getIntent().getStringExtra(EXTRA_PAYOUT));
        ((TextView) findViewById(R.id.orderWorkMode)).setText(
                getIntent().getStringExtra(EXTRA_WORK_MODE));
        findViewById(R.id.respondButton).setOnClickListener(view ->
                Toast.makeText(this, "Отклик будет доступен в следующих этапах", Toast.LENGTH_SHORT).show());
    }
}
