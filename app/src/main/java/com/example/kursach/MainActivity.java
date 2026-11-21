package com.example.kursach;

import android.os.Bundle;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private static final String[] CITIES = {"Любой город", "Минск", "Витебск", "Гомель", "Брест"};
    private EditText searchInput;
    private LinearLayout orderCards;
    private View emptyState;
    private View mapContent;
    private View[] mapMarkers;
    private boolean mapMode;
    private String selectedCity = "";
    private int minPrice;
    private int maxPrice;
    private boolean remoteOnly;
    private boolean onSiteOnly;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        searchInput = findViewById(R.id.searchInput);
        orderCards = findViewById(R.id.orderCards);
        emptyState = findViewById(R.id.emptyState);
        mapContent = findViewById(R.id.mapContent);
        mapMarkers = new View[]{findViewById(R.id.mapMarker0), findViewById(R.id.mapMarker1),
                findViewById(R.id.mapMarker2), findViewById(R.id.mapMarker3),
                findViewById(R.id.mapMarker4), findViewById(R.id.mapMarker5)};
        findViewById(R.id.listTab).setOnClickListener(v -> setMapMode(false));
        findViewById(R.id.mapTab).setOnClickListener(v -> setMapMode(true));
        for (int i = 0; i < mapMarkers.length; i++) {
            final int orderIndex = i;
            mapMarkers[i].setOnClickListener(v -> openOrderDetails(orderIndex));
            View card = orderCards.getChildAt(i);
            card.setOnClickListener(v -> openOrderDetails(orderIndex));
        }
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        findViewById(R.id.filterButton).setOnClickListener(v -> showFilters());
        applyFilters();
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void setMapMode(boolean showMap) {
        mapMode = showMap;
        orderCards.setVisibility(showMap ? View.GONE : View.VISIBLE);
        mapContent.setVisibility(showMap ? View.VISIBLE : View.GONE);
        findViewById(R.id.listTab).setBackgroundResource(showMap
                ? R.drawable.segmented_unselected : R.drawable.segmented_selected);
        findViewById(R.id.mapTab).setBackgroundResource(showMap
                ? R.drawable.segmented_selected : R.drawable.segmented_unselected);
    }

    private void openOrderDetails(int orderIndex) {
        View card = orderCards.getChildAt(orderIndex);
        if (!(card instanceof LinearLayout) || card.getVisibility() != View.VISIBLE) return;
        LinearLayout cardContent = (LinearLayout) card;
        String date = ((TextView) cardContent.getChildAt(0)).getText().toString();
        String title = ((TextView) cardContent.getChildAt(1)).getText().toString();
        String description = ((TextView) cardContent.getChildAt(2)).getText().toString();
        String tag = String.valueOf(card.getTag()).toLowerCase(Locale.ROOT);
        String city = "Город не указан";
        for (int i = 1; i < CITIES.length; i++) {
            if (tag.contains(CITIES[i].toLowerCase(Locale.ROOT))) {
                city = CITIES[i];
                break;
            }
        }
        String workMode = tag.contains("дистанц") || tag.contains("удалён")
                || tag.contains("удаленно") ? "Дистанционно" : "На месте";
        Intent intent = new Intent(this, OrderDetailsActivity.class);
        intent.putExtra(OrderDetailsActivity.EXTRA_DATE, date);
        intent.putExtra(OrderDetailsActivity.EXTRA_TITLE, title);
        intent.putExtra(OrderDetailsActivity.EXTRA_DESCRIPTION, description);
        intent.putExtra(OrderDetailsActivity.EXTRA_CITY, city);
        intent.putExtra(OrderDetailsActivity.EXTRA_WORK_MODE, workMode);
        startActivity(intent);
    }

    private void showFilters() {
        View content = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null);
        Spinner citySpinner = content.findViewById(R.id.citySpinner);
        CheckBox remoteCheckBox = content.findViewById(R.id.remoteCheckBox);
        CheckBox onSiteCheckBox = content.findViewById(R.id.onSiteCheckBox);
        EditText minPriceInput = content.findViewById(R.id.minPriceInput);
        EditText maxPriceInput = content.findViewById(R.id.maxPriceInput);

        ArrayAdapter<String> citiesAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CITIES);
        citiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(citiesAdapter);
        for (int i = 0; i < CITIES.length; i++) {
            if (CITIES[i].equalsIgnoreCase(selectedCity) || (selectedCity.isEmpty() && i == 0)) {
                citySpinner.setSelection(i);
                break;
            }
        }
        remoteCheckBox.setChecked(remoteOnly);
        onSiteCheckBox.setChecked(onSiteOnly);
        if (minPrice > 0) minPriceInput.setText(String.valueOf(minPrice));
        if (maxPrice > 0) maxPriceInput.setText(String.valueOf(maxPrice));

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Фильтры")
                .setView(content)
                .setPositiveButton("Применить", null)
                .setNeutralButton("Сбросить", null)
                .setNegativeButton("Отмена", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String minText = minPriceInput.getText().toString().trim();
                String maxText = maxPriceInput.getText().toString().trim();
                int newMin = parsePrice(minText);
                int newMax = parsePrice(maxText);
                if (newMin < 0 || newMax < 0) {
                    Toast.makeText(this, "Введите корректную цену", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newMin > 0 && newMax > 0 && newMin > newMax) {
                    Toast.makeText(this, "Цена «От» не может быть больше цены «До»", Toast.LENGTH_SHORT).show();
                    return;
                }
                selectedCity = citySpinner.getSelectedItemPosition() == 0
                        ? "" : CITIES[citySpinner.getSelectedItemPosition()].toLowerCase(Locale.ROOT);
                remoteOnly = remoteCheckBox.isChecked();
                onSiteOnly = onSiteCheckBox.isChecked();
                minPrice = newMin;
                maxPrice = newMax;
                applyFilters();
                dialog.dismiss();
            });
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                selectedCity = "";
                minPrice = 0;
                maxPrice = 0;
                remoteOnly = false;
                onSiteOnly = false;
                citySpinner.setSelection(0);
                remoteCheckBox.setChecked(false);
                onSiteCheckBox.setChecked(false);
                minPriceInput.setText("");
                maxPriceInput.setText("");
                applyFilters();
                dialog.dismiss();
            });
        });
        dialog.show();
    }

    private int parsePrice(String value) {
        if (value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private void applyFilters() {
        String query = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        Pattern pricePattern = Pattern.compile("\\d+");
        int visibleCount = 0;
        for (int i = 0; i < orderCards.getChildCount(); i++) {
            View card = orderCards.getChildAt(i);
            String text = String.valueOf(card.getTag()).toLowerCase(Locale.ROOT);
            Matcher matcher = pricePattern.matcher(text);
            int price = matcher.find() ? Integer.parseInt(matcher.group()) : 0;
            boolean isRemote = text.contains("дистанц") || text.contains("удалён") || text.contains("удаленно");
            boolean matchesWorkMode = (!remoteOnly && !onSiteOnly)
                    || (remoteOnly && isRemote) || (onSiteOnly && !isRemote);
            boolean matches = text.contains(query)
                    && (selectedCity.isEmpty() || text.contains(selectedCity))
                    && (minPrice == 0 || price >= minPrice)
                    && (maxPrice == 0 || price <= maxPrice)
                    && matchesWorkMode;
            card.setVisibility(matches ? View.VISIBLE : View.GONE);
            if (i < mapMarkers.length) mapMarkers[i].setVisibility(matches ? View.VISIBLE : View.GONE);
            if (matches) visibleCount++;
        }
        emptyState.setVisibility(visibleCount == 0 ? View.VISIBLE : View.GONE);
        emptyState.bringToFront();
    }
}
