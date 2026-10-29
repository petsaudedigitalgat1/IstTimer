package io.github.petsaudedigitalgat1.isttimer.ui;

import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.pm.PackageManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import java.util.Locale;
import android.util.Log;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import android.widget.Toast;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.res.ColorStateList;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import io.github.petsaudedigitalgat1.isttimer.R;
import io.github.petsaudedigitalgat1.isttimer.adapter.ActiveTimerAdapter;
import io.github.petsaudedigitalgat1.isttimer.viewmodel.TimerViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TimerViewModel timerViewModel;
    private ActiveTimerAdapter activeTimerAdapter;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private MaterialButton addTemplateButton;
    private ImageView toolbarLogo;
    private ImageView centerLogo;
    private ItemTouchHelper itemTouchHelper;
    private ScrollView infoContentScroll;
    private TextView infoDescriptionText;
    private TextView infoHighlightText;
    private ImageView infoImageView;
    private static final int REQUEST_NOTIFICATIONS = 1001;
    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LOCALE = "app_locale";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedLocale();
        super.onCreate(savedInstanceState);

        try {
            Log.d(TAG, "MainActivity iniciada");
            setContentView(R.layout.activity_main);

            timerViewModel = new ViewModelProvider(this).get(TimerViewModel.class);
            toolbar = findViewById(R.id.toolbar);
            recyclerView = findViewById(R.id.recyclerView_active_timers);
            addTemplateButton = findViewById(R.id.fab_add_timer);
            toolbarLogo = findViewById(R.id.image_view_app_logo_toolbar);
            centerLogo = findViewById(R.id.image_center_logo);
            infoContentScroll = findViewById(R.id.scroll_info_content);
            infoDescriptionText = findViewById(R.id.text_info_description);
            infoHighlightText = findViewById(R.id.text_info_highlight);
            infoDescriptionText.setLinksClickable(true);
            infoDescriptionText.setMovementMethod(LinkMovementMethod.getInstance());
            infoHighlightText.setLinksClickable(true);
            infoHighlightText.setMovementMethod(LinkMovementMethod.getInstance());
            infoImageView = findViewById(R.id.image_info_logo);

            setupDrawerMenu();
            requestNotificationPermissionIfNeeded();

            setupActiveTimerRecyclerView();
            addTemplateButton.setOnClickListener(v -> {
                // garantir que o logo volte para o canto ao iniciar criação de novo timer
                if (centerLogo != null) centerLogo.setVisibility(View.GONE);
                if (toolbarLogo != null) toolbarLogo.setVisibility(View.VISIBLE);
                showAddTemplateDialog();
            });

            timerViewModel.getActiveTimers().observe(this, activeTimers -> {
                activeTimerAdapter.setActiveTimers(activeTimers);
                boolean empty = activeTimers == null || activeTimers.isEmpty();
                updateAddButtonAppearance(empty);
                // mostrar logo central quando não houver timers, caso contrário mostrar o logo no toolbar
                if (centerLogo != null && toolbarLogo != null) {
                    if (empty) {
                        centerLogo.setVisibility(View.VISIBLE);
                        toolbarLogo.setVisibility(View.INVISIBLE);
                    } else {
                        centerLogo.setVisibility(View.GONE);
                        toolbarLogo.setVisibility(View.VISIBLE);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Erro no onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Erro ao iniciar a aplicação", Toast.LENGTH_LONG).show();
        }
    }

    private String getPrivacyUrl() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String locale = prefs.getString(KEY_LOCALE, "system");
            String lang;
            if (locale == null || "system".equals(locale)) {
                lang = Locale.getDefault().getLanguage();
            } else {
                lang = locale;
            }

            Log.d(TAG, "getPrivacyUrl - resolved language: " + lang + " (stored=" + locale + ")");

            if (lang != null) {
                if (lang.startsWith("en")) {
                    return "https://sites.google.com/view/isttimer/start";
                } else if (lang.startsWith("es")) {
                    return "https://sites.google.com/view/ist-timer/acuerdo-de-usuario";
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Erro ao resolver URL de privacidade: " + e.getMessage(), e);
        }
        // default Portuguese
        return "https://sites.google.com/view/temporizadorist/in%C3%ADcio";
    }

    private void setupDrawerMenu() {
        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.navigation_view);

        setSupportActionBar(toolbar);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.menu_inicio,
                R.string.menu_inicio
        );

        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            handleDrawerItemClick(item);
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    private void handleDrawerItemClick(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_inicio) {
            showMainTimers();
        } else if (itemId == R.id.nav_quem_somos) {
            showInfoScreen(
                    getString(R.string.menu_quem_somos),
                    getString(R.string.quem_somos_title),
                    getString(R.string.quem_somos_content)
            );
            if (infoImageView != null) {
                // esconder título desnecessário e subir a imagem
                if (infoDescriptionText != null) infoDescriptionText.setVisibility(View.GONE);
                int resId = getResources().getIdentifier("quem_somos_logo", "drawable", getPackageName());
                if (resId != 0) {
                    infoImageView.setImageResource(resId);
                    // remover margin top para subir imagem
                    if (infoImageView.getLayoutParams() instanceof LinearLayout.LayoutParams) {
                        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) infoImageView.getLayoutParams();
                        lp.topMargin = 0;
                        infoImageView.setLayoutParams(lp);
                    }
                    infoImageView.setVisibility(View.VISIBLE);
                } else {
                    infoImageView.setVisibility(View.GONE);
                }
            }
        } else if (itemId == R.id.nav_converse_conosco) {
            showInfoScreen(
                    getString(R.string.menu_converse_conosco),
                    getString(R.string.contact_description),
                    getString(R.string.contact_email)
            );
            if (infoImageView != null) infoImageView.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_termo_privacidade) {
            String url = getPrivacyUrl();
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Erro ao abrir link: " + e.getMessage(), e);
                Toast.makeText(this, "Não foi possível abrir o link", Toast.LENGTH_SHORT).show();
            }
            if (infoImageView != null) infoImageView.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_idioma) {
            showLanguageSelectionDialog();
            if (infoImageView != null) infoImageView.setVisibility(View.GONE);
        } else if (itemId == R.id.nav_mais_informacoes) {
            showInfoScreen(
                    getString(R.string.menu_mais_informacoes),
                    getString(R.string.more_info_content),
                    getString(R.string.more_info_license)
            );
            if (infoImageView != null) infoImageView.setVisibility(View.GONE);
            
        }
    }

    private void showMainTimers() {
        toolbar.setTitle("");
        recyclerView.setVisibility(View.VISIBLE);
        addTemplateButton.setVisibility(View.VISIBLE);
        infoContentScroll.setVisibility(View.GONE);
        boolean empty = true;
        try {
            java.util.List<?> active = timerViewModel.getActiveTimers().getValue();
            empty = active == null || active.isEmpty();
        } catch (Exception e) {
            // se algo der errado ao acessar viewmodel, assume vazio
            empty = true;
        }

        if (centerLogo != null && toolbarLogo != null) {
            if (empty) {
                centerLogo.setVisibility(View.VISIBLE);
                toolbarLogo.setVisibility(View.INVISIBLE);
            } else {
                centerLogo.setVisibility(View.GONE);
                toolbarLogo.setVisibility(View.VISIBLE);
            }
        }
        updateAddButtonAppearance(empty);
    }

    private void showInfoScreen(String title, String description, String highlight) {
        toolbar.setTitle(title);
        recyclerView.setVisibility(View.GONE);
        addTemplateButton.setVisibility(View.GONE);
        infoContentScroll.setVisibility(View.VISIBLE);
        if (centerLogo != null) centerLogo.setVisibility(View.GONE);
        if (toolbarLogo != null) toolbarLogo.setVisibility(View.GONE);
        // garantir estado padrão: descrição visível, imagem oculta
        infoDescriptionText.setVisibility(View.VISIBLE);
        if (infoImageView != null) infoImageView.setVisibility(View.GONE);
        infoHighlightText.setVisibility(View.VISIBLE);
        infoDescriptionText.setBackground(null);
        infoDescriptionText.setPadding(0, 0, 0, 0);
        infoDescriptionText.setText(description);
        
        // Compatibilidade com diferentes versões do Android
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            infoHighlightText.setText(Html.fromHtml(highlight, Html.FROM_HTML_MODE_LEGACY));
        } else {
            infoHighlightText.setText(Html.fromHtml(highlight));
        }
    }

    private void applySavedLocale() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String locale = prefs.getString(KEY_LOCALE, "system");
            LocaleListCompat appLocales;
            if (locale == null || "system".equals(locale)) {
                appLocales = LocaleListCompat.getEmptyLocaleList();
            } else {
                appLocales = LocaleListCompat.forLanguageTags(locale);
            }
            AppCompatDelegate.setApplicationLocales(appLocales);
        } catch (Exception e) {
            Log.e(TAG, "Erro ao aplicar localidade salva: " + e.getMessage(), e);
        }
    }

    private void showLanguageSelectionDialog() {
        final String[] codes = new String[] {"system", "pt", "en", "es"};

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String current = prefs.getString(KEY_LOCALE, "system");
        int checked = 0;
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(current)) { checked = i; break; }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_language_selection, null);
        builder.setView(view);

        RadioGroup radioGroup = view.findViewById(R.id.radio_group_languages);
        RadioButton radioDevice = view.findViewById(R.id.radio_device_default);
        RadioButton radioPortuguese = view.findViewById(R.id.radio_portuguese);
        RadioButton radioEnglish = view.findViewById(R.id.radio_english);
        RadioButton radioSpanish = view.findViewById(R.id.radio_spanish);
        Button cancelButton = view.findViewById(R.id.button_cancel_language);
        Button okButton = view.findViewById(R.id.button_ok_language);

        switch (checked) {
            case 1:
                radioGroup.check(radioPortuguese.getId());
                break;
            case 2:
                radioGroup.check(radioEnglish.getId());
                break;
            case 3:
                radioGroup.check(radioSpanish.getId());
                break;
            default:
                radioGroup.check(radioDevice.getId());
                break;
        }

        final int[] selected = new int[] { checked };
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == radioDevice.getId()) {
                selected[0] = 0;
            } else if (checkedId == radioPortuguese.getId()) {
                selected[0] = 1;
            } else if (checkedId == radioEnglish.getId()) {
                selected[0] = 2;
            } else if (checkedId == radioSpanish.getId()) {
                selected[0] = 3;
            }
        });

        AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> {
            String selectedCode = codes[selected[0]];
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_LOCALE, selectedCode);
            editor.apply();
            Log.d(TAG, "Idioma selecionado (salvando): index=" + selected[0] + " code=" + selectedCode);
            Toast.makeText(this, "Idioma selecionado: " + selectedCode, Toast.LENGTH_SHORT).show();
            LocaleListCompat appLocales = "system".equals(selectedCode)
                    ? LocaleListCompat.getEmptyLocaleList()
                    : LocaleListCompat.forLanguageTags(selectedCode);
            AppCompatDelegate.setApplicationLocales(appLocales);
            // Ensure Locale.getDefault() and resource configuration reflect the chosen locale
            try {
                if (!"system".equals(selectedCode)) {
                    Locale chosen = Locale.forLanguageTag(selectedCode);
                    Locale.setDefault(chosen);
                    Resources res = getResources();
                    Configuration conf = res.getConfiguration();
                    conf.setLocale(chosen);
                    res.updateConfiguration(conf, res.getDisplayMetrics());
                }
            } catch (Exception e) {
                Log.w(TAG, "Falha ao aplicar locale globalmente: " + e.getMessage(), e);
            }
            dialog.dismiss();
            recreate();
        });

        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getOnBackPressedDispatcher().onBackPressed();
        } else {
            super.onBackPressed();
        }
    }

    private void setupActiveTimerRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerView_active_timers);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        activeTimerAdapter = new ActiveTimerAdapter(this,
                (activeTimer, position) -> timerViewModel.removeActiveTimer(activeTimer, position));
        recyclerView.setAdapter(activeTimerAdapter);

        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                0
        ) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int fromPosition = viewHolder.getBindingAdapterPosition();
                int toPosition = target.getBindingAdapterPosition();
                if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
                    return false;
                }
                timerViewModel.moveActiveTimer(fromPosition, toPosition);
                activeTimerAdapter.notifyItemMoved(fromPosition, toPosition);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // Não usamos swipe
            }

            @Override
            public boolean isLongPressDragEnabled() {
                // Deixa o proprio ItemTouchHelper detectar o toque e segure no
                // bloco inteiro do timer, usando o tempo padrao do sistema
                // (ViewConfiguration.getLongPressTimeout(), ~500ms) - o mesmo
                // usado em icones de tela inicial, respeitando tambem o ajuste
                // de "Toque e segure" das opcoes de acessibilidade do usuario.
                return true;
            }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    View draggedView = viewHolder.itemView;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        draggedView.performHapticFeedback(HapticFeedbackConstants.DRAG_START);
                    } else {
                        draggedView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    }
                }
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void updateAddButtonAppearance(boolean showLargeCenterButton) {
        if (addTemplateButton == null) {
            return;
        }
        android.view.ViewGroup.LayoutParams lp = addTemplateButton.getLayoutParams();

        int sizePx = dpToPx(showLargeCenterButton ? 120 : 80);

        if (lp instanceof FrameLayout.LayoutParams) {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) lp;
            params.width = sizePx;
            params.height = sizePx;
            if (showLargeCenterButton) {
                params.gravity = android.view.Gravity.CENTER;
                params.leftMargin = 0;
                params.topMargin = 0;
                params.rightMargin = 0;
                params.bottomMargin = 0;
                addTemplateButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 44);
                addTemplateButton.setCornerRadius(dpToPx(24));
            } else {
                params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.END;
                params.leftMargin = 0;
                params.topMargin = 0;
                params.rightMargin = dpToPx(16);
                params.bottomMargin = dpToPx(16);
                addTemplateButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 32);
                addTemplateButton.setCornerRadius(dpToPx(20));
            }
            addTemplateButton.setLayoutParams(params);
        } else if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) lp;
            params.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = dpToPx(56); // keep height for pill
            if (showLargeCenterButton) {
                params.gravity = android.view.Gravity.CENTER;
                // usar fonte menor quando a tela estiver sem cronômetros para caber no texto
                addTemplateButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
                addTemplateButton.setCornerRadius(dpToPx(24));
            } else {
                params.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL;
                addTemplateButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                addTemplateButton.setCornerRadius(dpToPx(28));
            }
            addTemplateButton.setLayoutParams(params);
        } else if (lp != null) {
            lp.width = android.view.ViewGroup.LayoutParams.MATCH_PARENT;
            lp.height = dpToPx(56);
            addTemplateButton.setLayoutParams(lp);
        }
    }

    private void showAddTemplateDialog() {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_template, null);
            builder.setView(view);

            EditText patientNameEditText = view.findViewById(R.id.editText_test_name);
            TextView hivDurationTextView = view.findViewById(R.id.text_duration_hiv);
            TextView sifilisDurationTextView = view.findViewById(R.id.text_duration_sifilis);
            TextView hepatiteBDurationTextView = view.findViewById(R.id.text_duration_hepatite_b);
            TextView hepatiteCDurationTextView = view.findViewById(R.id.text_duration_hepatite_c);
            LinearLayout hivDurationContainer = view.findViewById(R.id.duration_container_hiv);
            LinearLayout sifilisDurationContainer = view.findViewById(R.id.duration_container_sifilis);
            LinearLayout hepatiteBDurationContainer = view.findViewById(R.id.duration_container_hepatite_b);
            LinearLayout hepatiteCDurationContainer = view.findViewById(R.id.duration_container_hepatite_c);
            Button startTestButton = view.findViewById(R.id.button_start_test);
            Button cancelButton = view.findViewById(R.id.button_cancel);
            Chip hivChip = view.findViewById(R.id.chip_hiv);
                Chip hepatiteBChip = view.findViewById(R.id.chip_hepatite_b);
                Chip hepatiteCChip = view.findViewById(R.id.chip_hepatite_c);
            Chip sifilisChip = view.findViewById(R.id.chip_sifilis);

            if (patientNameEditText == null || hivDurationTextView == null || sifilisDurationTextView == null
                    || hepatiteBDurationTextView == null || hepatiteCDurationTextView == null || startTestButton == null
                    || hivChip == null || hepatiteBChip == null || hepatiteCChip == null || sifilisChip == null) {
                Toast.makeText(this, "Erro ao abrir formulário", Toast.LENGTH_SHORT).show();
                return;
            }

            configureSelectableChip(hivChip);
                configureSelectableChip(hepatiteBChip);
                configureSelectableChip(hepatiteCChip);
            configureSelectableChip(sifilisChip);

            // Arrays para armazenar os tempos selecionados
            int[] selectedTimes = new int[4]; // HIV, SÍFILIS, HEPATITE B, HEPATITE C
            selectedTimes[0] = 0;
            selectedTimes[1] = 0;
            selectedTimes[2] = 0;
            selectedTimes[3] = 0;

            // Configurar listeners para os campos de tempo
            hivDurationContainer.setOnClickListener(v -> {
                showTimePicker(selectedTimes, 0, hivDurationTextView);
            });
            sifilisDurationContainer.setOnClickListener(v -> {
                showTimePicker(selectedTimes, 1, sifilisDurationTextView);
            });
            hepatiteBDurationContainer.setOnClickListener(v -> {
                showTimePicker(selectedTimes, 2, hepatiteBDurationTextView);
            });
            hepatiteCDurationContainer.setOnClickListener(v -> {
                showTimePicker(selectedTimes, 3, hepatiteCDurationTextView);
            });

            AlertDialog dialog = builder.show();

            startTestButton.setOnClickListener(v -> {
                String patientName = patientNameEditText.getText().toString().trim();

                java.util.List<Integer> selectedChipIds = new java.util.ArrayList<>();
                if (hivChip.isChecked()) {
                    selectedChipIds.add(R.id.chip_hiv);
                }
                if (sifilisChip.isChecked()) {
                    selectedChipIds.add(R.id.chip_sifilis);
                }
                if (hepatiteBChip.isChecked()) {
                    selectedChipIds.add(R.id.chip_hepatite_b);
                }
                if (hepatiteCChip.isChecked()) {
                    selectedChipIds.add(R.id.chip_hepatite_c);
                }

                if (patientName.isEmpty() || selectedChipIds.isEmpty()) {
                    Toast.makeText(this, "Preencha todos os campos", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    int index = 0;
                    for (Integer chipId : selectedChipIds) {
                        Chip selectedChip = view.findViewById(chipId);
                        if (selectedChip != null) {
                            String testType = selectedChip.getText().toString();
                            int timeIndex = getTimeIndexForChip(chipId);
                            int durationInMinutes = selectedTimes[timeIndex];

                            if (durationInMinutes == 0) {
                                Toast.makeText(this, "Selecione o tempo para cada teste", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            timerViewModel.startNewTimer(patientName, testType, durationInMinutes);
                        }
                    }
                    dialog.dismiss();
                } catch (Exception e) {
                    Toast.makeText(this, "Erro ao iniciar timer", Toast.LENGTH_SHORT).show();
                }
            });
            if (cancelButton != null) {
                cancelButton.setOnClickListener(v -> {
                    dialog.dismiss();
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Erro ao abrir diálogo de novo teste", e);
            Toast.makeText(this, "Não foi possível abrir o formulário", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTimePicker(int[] selectedTimes, int index, TextView targetTextView) {
        final int[] options = {5, 10, 15, 20, 25};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Selecione o tempo")
            .setSingleChoiceItems(
                new String[]{"5 minutos", "10 minutos", "15 minutos", "20 minutos", "25 minutos"},
                -1,
                (dialog, which) -> {
                    selectedTimes[index] = options[which];
                    // exibir abreviação no campo após seleção
                    targetTextView.setText(options[which] + " min");
                    dialog.dismiss();
                })
            .show();
    }

    private int getTimeIndexForChip(int chipId) {
        if (chipId == R.id.chip_hiv) {
            return 0;
        }
        if (chipId == R.id.chip_sifilis) {
            return 1;
        }
        if (chipId == R.id.chip_hepatite_b) {
            return 2;
        }
        if (chipId == R.id.chip_hepatite_c) {
            return 3;
        }
        return -1;
    }

    private void configureSelectableChip(Chip chip) {
        chip.setCheckable(true);
        chip.setClickable(true);
        chip.setFocusable(true);
        chip.setChecked(false);
        chip.setElevation(2f);
        chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            chip.setElevation(isChecked ? 10f : 2f);
            chip.setAlpha(isChecked ? 1.0f : 0.92f);
        });
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                REQUEST_NOTIFICATIONS
        );
    }
}
