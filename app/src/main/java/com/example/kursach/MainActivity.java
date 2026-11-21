package com.example.kursach;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        EditText searchInput = findViewById(R.id.searchInput);
        LinearLayout orderCards = findViewById(R.id.orderCards);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase();
                for (int i = 0; i < orderCards.getChildCount(); i++) {
                    View card = orderCards.getChildAt(i);
                    String searchableText = String.valueOf(card.getTag()).toLowerCase();
                    card.setVisibility(searchableText.contains(query) ? View.VISIBLE : View.GONE);
                }
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        findViewById(R.id.filterButton).setOnClickListener(v ->
                Toast.makeText(this, "Фильтры будут добавлены следующим шагом", Toast.LENGTH_SHORT).show());
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
