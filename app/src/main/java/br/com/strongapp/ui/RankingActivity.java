package br.com.strongapp.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import br.com.strongapp.data.SessionManager;
import br.com.strongapp.databinding.ActivityRankingBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Ranking da comunidade, com as mesmas três métricas da versão web.
 *
 * <p>Atenção: a API do StrongApp não expõe ranking entre usuários, então esta tela
 * usa a mesma lista fictícia de demonstração da versão web
 * (src/components/UserRankingChart.tsx). Nenhum número aqui vem do banco.
 */
public class RankingActivity extends AppCompatActivity {

    private static final String[] METRICS = {
            "Treinos Concluídos", "Sequência de Exercícios", "Recorde Semanal"
    };

    private ActivityRankingBinding binding;
    private RankingAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRankingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        adapter = new RankingAdapter();
        binding.list.setLayoutManager(new LinearLayoutManager(this));
        binding.list.setAdapter(adapter);

        binding.metricInput.setSimpleItems(METRICS);
        binding.metricInput.setText(METRICS[0], false);
        binding.metricInput.setOnItemClickListener((parent, view, position, id) -> show(position));

        show(0);
    }

    private void show(int metric) {
        adapter.submit(dataFor(metric), SessionManager.get(this).getName());
    }

    private static List<RankingUser> dataFor(int metric) {
        switch (metric) {
            case 1:
                return build("dias seguidos",
                        new String[]{"Rafael Mendes", "Lucia Ferreira", "Diego Rocha", "Camila Torres", "Bruno Alves"},
                        new int[]{12, 10, 8, 7, 6});
            case 2:
                return build("treinos esta semana",
                        new String[]{"Fernanda Dias", "Ricardo Souza", "Juliana Reis", "Marcos Antonio", "Beatriz Moura"},
                        new int[]{8, 7, 6, 5, 4});
            default:
                return build("treinos completos",
                        new String[]{"Carlos Silva", "Ana Costa", "João Santos", "Maria Oliveira", "Pedro Lima"},
                        new int[]{25, 22, 19, 17, 15});
        }
    }

    private static List<RankingUser> build(String subtitle, String[] names, int[] values) {
        List<RankingUser> list = new ArrayList<>(names.length);
        for (int i = 0; i < names.length; i++) {
            list.add(new RankingUser(i + 1, names[i], values[i], subtitle));
        }
        return list;
    }
}
