package br.com.strongapp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import br.com.strongapp.R;
import br.com.strongapp.databinding.SheetTimerBinding;

import java.util.Locale;

/** Cronômetro de descanso com iniciar, pausar e zerar (RF09). */
public class TimerBottomSheet extends BottomSheetDialogFragment {

    private SheetTimerBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private boolean running = false;
    private long elapsedMillis = 0L;
    private long startedAt = 0L;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (!running || binding == null) return;
            render(elapsedMillis + (SystemClock.elapsedRealtime() - startedAt));
            handler.postDelayed(this, 200L);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        render(0L);

        binding.startPause.setOnClickListener(v -> {
            if (running) {
                pause();
            } else {
                start();
            }
        });

        binding.reset.setOnClickListener(v -> {
            pause();
            elapsedMillis = 0L;
            render(0L);
        });
    }

    private void start() {
        running = true;
        startedAt = SystemClock.elapsedRealtime();
        binding.startPause.setText(R.string.pause);
        handler.post(tick);
    }

    private void pause() {
        if (running) {
            elapsedMillis += SystemClock.elapsedRealtime() - startedAt;
            running = false;
        }
        handler.removeCallbacks(tick);
        if (binding != null) {
            binding.startPause.setText(R.string.start);
        }
    }

    private void render(long millis) {
        long totalSeconds = millis / 1000L;
        binding.clock.setText(String.format(Locale.getDefault(), "%02d:%02d",
                totalSeconds / 60L, totalSeconds % 60L));
    }

    @Override
    public void onDestroyView() {
        handler.removeCallbacks(tick);
        super.onDestroyView();
        binding = null;
    }
}
