package br.com.strongapp.ui;

import androidx.annotation.DrawableRes;

import br.com.strongapp.R;
import br.com.strongapp.model.AchievementStats;

import java.util.ArrayList;
import java.util.List;

/**
 * Conquista do usuário. As seis regras são as mesmas da versão web
 * (src/components/AchievementsDisplay.tsx), calculadas sobre GET /profile/achievement-stats.
 */
public class Achievement {

    public final String name;
    public final String description;
    @DrawableRes public final int icon;
    public final boolean unlocked;
    /** Progresso atual e alvo; {@code total == 0} significa conquista sem barra. */
    public final int progress;
    public final int total;

    private Achievement(String name, String description, int icon, boolean unlocked, int progress, int total) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.unlocked = unlocked;
        this.progress = progress;
        this.total = total;
    }

    public static List<Achievement> from(AchievementStats stats) {
        int workouts = stats.totalWorkouts;
        int exercises = stats.totalExerciseChecks;
        int diary = stats.totalDiaryEntries;
        int streak = stats.currentStreak;

        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("Primeiro Passo", "Complete seu primeiro treino",
                R.drawable.ic_trophy, exercises > 0, 0, 0));
        list.add(new Achievement("Criador de Treinos", "Crie 5 treinos personalizados",
                R.drawable.ic_target, workouts >= 5, Math.min(workouts, 5), 5));
        list.add(new Achievement("Centurião", "Complete 100 exercícios",
                R.drawable.ic_medal, exercises >= 100, Math.min(exercises, 100), 100));
        list.add(new Achievement("Dedicado", "Mantenha uma sequência de 7 dias",
                R.drawable.ic_bolt, streak >= 7, Math.min(streak, 7), 7));
        list.add(new Achievement("Guardião do Diário", "Faça 10 anotações no diário",
                R.drawable.ic_star, diary >= 10, Math.min(diary, 10), 10));
        list.add(new Achievement("Maratonista", "Complete 30 dias consecutivos",
                R.drawable.ic_award, streak >= 30, Math.min(streak, 30), 30));
        return list;
    }
}
