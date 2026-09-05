package br.com.strongapp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import br.com.strongapp.R;
import br.com.strongapp.databinding.ActivityMainBinding;
import br.com.strongapp.util.ThemeMode;

/** Casca do app: navegação inferior entre Início, Treinos, Exercícios e Perfil. */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.themeButton.setOnClickListener(v -> ThemeMode.toggle(this));

        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return show(new DashboardFragment());
            } else if (id == R.id.nav_workouts) {
                return show(new WorkoutsFragment());
            } else if (id == R.id.nav_exercises) {
                return show(new ExercisesFragment());
            } else if (id == R.id.nav_profile) {
                return show(new ProfileFragment());
            }
            return false;
        });

        if (savedInstanceState == null) {
            binding.bottomNav.setSelectedItemId(R.id.nav_home);
        }
    }

    /** Permite que um fragmento troque de aba (usado pelos atalhos do dashboard). */
    public void selectTab(int itemId) {
        binding.bottomNav.setSelectedItemId(itemId);
    }

    private boolean show(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        return true;
    }
}
