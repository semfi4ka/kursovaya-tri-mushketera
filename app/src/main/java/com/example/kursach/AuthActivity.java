package com.example.kursach;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONException;
import org.json.JSONObject;

public class AuthActivity extends AppCompatActivity {
    private ScrollView authRoot;
    private LinearLayout loginPanel;
    private LinearLayout registrationPanel;
    private LinearLayout registrationStepOne;
    private LinearLayout registrationStepTwo;
    private View registrationProgressOne;
    private View registrationProgressTwo;
    private TextView authTitle;
    private TextView authSubtitle;
    private TextView registrationStepLabel;
    private TextView registerDateOfBirth;
    private EditText loginIdentifier;
    private EditText loginPassword;
    private EditText registerSurname;
    private EditText registerFirstName;
    private EditText registerPatronymic;
    private EditText registerPhone;
    private EditText registerEmail;
    private EditText registerUsername;
    private EditText registerPassword;
    private Calendar selectedBirthDate;
    private final ExecutorService apiExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);

        authRoot = findViewById(R.id.authRoot);
        loginPanel = findViewById(R.id.loginPanel);
        registrationPanel = findViewById(R.id.registrationPanel);
        registrationStepOne = findViewById(R.id.registrationStepOne);
        registrationStepTwo = findViewById(R.id.registrationStepTwo);
        authTitle = findViewById(R.id.authTitle);
        authSubtitle = findViewById(R.id.authSubtitle);
        registrationStepLabel = findViewById(R.id.registrationStepLabel);
        registrationProgressOne = findViewById(R.id.registrationProgressOne);
        registrationProgressTwo = findViewById(R.id.registrationProgressTwo);
        loginIdentifier = findViewById(R.id.loginIdentifier);
        loginPassword = findViewById(R.id.loginPassword);
        registerSurname = findViewById(R.id.registerSurname);
        registerFirstName = findViewById(R.id.registerFirstName);
        registerPatronymic = findViewById(R.id.registerPatronymic);
        registerDateOfBirth = findViewById(R.id.registerDateOfBirth);
        registerPhone = findViewById(R.id.registerPhone);
        registerEmail = findViewById(R.id.registerEmail);
        registerUsername = findViewById(R.id.registerUsername);
        registerPassword = findViewById(R.id.registerPassword);

        findViewById(R.id.openRegistration).setOnClickListener(view -> showRegistration());
        findViewById(R.id.openLogin).setOnClickListener(view -> showLogin());
        findViewById(R.id.loginButton).setOnClickListener(view -> validateLoginForm());
        findViewById(R.id.guestButton).setOnClickListener(view -> openGuestMode());
        findViewById(R.id.nextRegistrationStep).setOnClickListener(view -> validateProfileStep());
        findViewById(R.id.previousRegistrationStep).setOnClickListener(view -> showRegistrationStep(1));
        findViewById(R.id.registerButton).setOnClickListener(view -> validateAccountStep());
        registerDateOfBirth.setOnClickListener(view -> showDatePicker());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (registrationPanel.getVisibility() == View.VISIBLE) {
                    if (registrationStepTwo.getVisibility() == View.VISIBLE) {
                        showRegistrationStep(1);
                    } else {
                        showLogin();
                    }
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(authRoot, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left + dp(24), systemBars.top + dp(18),
                    systemBars.right + dp(24), systemBars.bottom + dp(28));
            return insets;
        });
    }

    private void showRegistration() {
        loginPanel.setVisibility(View.GONE);
        registrationPanel.setVisibility(View.VISIBLE);
        showRegistrationStep(1);
    }

    private void showLogin() {
        registrationPanel.setVisibility(View.GONE);
        loginPanel.setVisibility(View.VISIBLE);
        authTitle.setText("Войдите в аккаунт");
        authSubtitle.setText("Находите подработку, которая подходит именно вам");
        authRoot.smoothScrollTo(0, 0);
    }

    private void showRegistrationStep(int step) {
        boolean firstStep = step == 1;
        registrationStepOne.setVisibility(firstStep ? View.VISIBLE : View.GONE);
        registrationStepTwo.setVisibility(firstStep ? View.GONE : View.VISIBLE);
        findViewById(R.id.previousRegistrationStep).setVisibility(firstStep ? View.GONE : View.VISIBLE);
        registrationStepLabel.setText(firstStep ? "ШАГ 1 ИЗ 2" : "ШАГ 2 ИЗ 2");
        registrationProgressOne.setBackgroundResource(R.drawable.auth_progress_active);
        registrationProgressTwo.setBackgroundResource(firstStep
                ? R.drawable.auth_progress_inactive : R.drawable.auth_progress_active);
        authTitle.setText(firstStep ? "Личные данные" : "Данные для входа");
        authSubtitle.setText(firstStep
                ? "Укажите имя и дату рождения"
                : "Добавьте контакты и придумайте данные для входа");
        authRoot.smoothScrollTo(0, 0);
    }

    private void showDatePicker() {
        Calendar initialDate = selectedBirthDate == null ? Calendar.getInstance() : selectedBirthDate;
        if (selectedBirthDate == null) initialDate.add(Calendar.YEAR, -18);
        DatePickerDialog dialog = new DatePickerDialog(this, (picker, year, month, day) -> {
            selectedBirthDate = Calendar.getInstance();
            selectedBirthDate.set(year, month, day, 0, 0, 0);
            selectedBirthDate.set(Calendar.MILLISECOND, 0);
            registerDateOfBirth.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    .format(selectedBirthDate.getTime()));
            registerDateOfBirth.setTextColor(getColor(R.color.nav_active));
        }, initialDate.get(Calendar.YEAR), initialDate.get(Calendar.MONTH),
                initialDate.get(Calendar.DAY_OF_MONTH));
        dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        dialog.show();
    }

    private void validateLoginForm() {
        String identifier = loginIdentifier.getText().toString().trim();
        String password = loginPassword.getText().toString();

        if (identifier.isEmpty()) {
            loginIdentifier.setError("Введите логин или email");
            loginIdentifier.requestFocus();
            return;
        }
        if (identifier.contains("@") && !isValidEmail(identifier)) {
            loginIdentifier.setError("Введите корректный email или логин");
            loginIdentifier.requestFocus();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            loginPassword.setError("Введите пароль");
            loginPassword.requestFocus();
            return;
        }
        JSONObject payload = new JSONObject();
        try {
            payload.put("identifier", identifier);
            payload.put("password", password);
        } catch (JSONException exception) {
            Toast.makeText(this, "Не удалось подготовить запрос", Toast.LENGTH_SHORT).show();
            return;
        }
        sendAuthRequest("/api/auth/login", payload, findViewById(R.id.loginButton));
    }

    private void openGuestMode() {
        startActivity(new Intent(this, MainActivity.class));
    }

    private void validateProfileStep() {
        String surname = registerSurname.getText().toString().trim();
        String firstName = registerFirstName.getText().toString().trim();

        if (surname.isEmpty()) {
            registerSurname.setError("Введите фамилию");
            registerSurname.requestFocus();
            return;
        }
        if (firstName.isEmpty()) {
            registerFirstName.setError("Введите имя");
            registerFirstName.requestFocus();
            return;
        }
        if (selectedBirthDate == null) {
            Toast.makeText(this, "Выберите дату рождения", Toast.LENGTH_SHORT).show();
            return;
        }
        showRegistrationStep(2);
    }

    private void validateAccountStep() {
        String username = registerUsername.getText().toString().trim();
        String phone = registerPhone.getText().toString().trim();
        String email = registerEmail.getText().toString().trim();
        String password = registerPassword.getText().toString();

        if (username.length() < 3) {
            registerUsername.setError("Логин должен содержать не менее 3 символов");
            registerUsername.requestFocus();
            return;
        }
        if (phone.isEmpty()) {
            registerPhone.setError("Введите номер телефона");
            registerPhone.requestFocus();
            return;
        }
        if (!isValidEmail(email)) {
            registerEmail.setError("Введите корректный email");
            registerEmail.requestFocus();
            return;
        }
        if (password.length() < 8) {
            registerPassword.setError("Пароль должен содержать не менее 8 символов");
            registerPassword.requestFocus();
            return;
        }

        JSONObject payload = new JSONObject();
        try {
            payload.put("surname", registerSurname.getText().toString().trim());
            payload.put("firstName", registerFirstName.getText().toString().trim());
            payload.put("patronymic", registerPatronymic.getText().toString().trim());
            payload.put("birthDate", new SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    .format(selectedBirthDate.getTime()));
            payload.put("username", username);
            payload.put("phone", phone);
            payload.put("email", email);
            payload.put("password", password);
        } catch (JSONException exception) {
            Toast.makeText(this, "Не удалось подготовить запрос", Toast.LENGTH_SHORT).show();
            return;
        }
        sendAuthRequest("/api/auth/register", payload, findViewById(R.id.registerButton));
    }

    private void sendAuthRequest(String path, JSONObject payload, TextView submitButton) {
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.6f);
        apiExecutor.execute(() -> {
            AuthApiClient.Result result = AuthApiClient.postJson(path, payload);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                submitButton.setEnabled(true);
                submitButton.setAlpha(1f);
                if (!result.successful) {
                    Toast.makeText(this, result.message, Toast.LENGTH_LONG).show();
                    return;
                }
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("user_id", result.userId);
                intent.putExtra("username", result.username);
                startActivity(intent);
                finish();
            });
        });
    }

    private boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        apiExecutor.shutdownNow();
        super.onDestroy();
    }
}
