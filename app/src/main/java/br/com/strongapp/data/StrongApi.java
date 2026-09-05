package br.com.strongapp.data;

import br.com.strongapp.model.ApiMessage;
import br.com.strongapp.model.AuthResponse;
import br.com.strongapp.model.AchievementStats;
import br.com.strongapp.model.CheckRequest;
import br.com.strongapp.model.CreateWorkoutRequest;
import br.com.strongapp.model.DiaryEntry;
import br.com.strongapp.model.DiaryRequest;
import br.com.strongapp.model.Exercise;
import br.com.strongapp.model.ExerciseCheck;
import br.com.strongapp.model.ExportBundle;
import br.com.strongapp.model.LoginRequest;
import br.com.strongapp.model.ProfileStats;
import br.com.strongapp.model.ProfileUpdateRequest;
import br.com.strongapp.model.RegisterRequest;
import br.com.strongapp.model.ShareRequest;
import br.com.strongapp.model.User;
import br.com.strongapp.model.Workout;
import br.com.strongapp.model.WorkoutProgress;
import br.com.strongapp.model.WorkoutShare;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** Contrato da API Laravel do StrongApp (backend/routes/api.php). */
public interface StrongApi {

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterRequest body);

    @POST("auth/logout")
    Call<ApiMessage> logout();

    @GET("auth/user")
    Call<User> currentUser();

    @GET("profile")
    Call<User> profile();

    @PUT("profile")
    Call<User> updateProfile(@Body ProfileUpdateRequest body);

    @GET("profile/stats")
    Call<ProfileStats> profileStats();

    @GET("profile/achievement-stats")
    Call<AchievementStats> achievementStats();

    @GET("export")
    Call<ExportBundle> export();

    @GET("exercises")
    Call<List<Exercise>> exercises();

    @GET("workouts")
    Call<List<Workout>> workouts();

    @GET("workouts/{id}")
    Call<Workout> workout(@Path("id") String id);

    @POST("workouts")
    Call<Workout> createWorkout(@Body CreateWorkoutRequest body);

    @PUT("workouts/{id}")
    Call<Workout> updateWorkout(@Path("id") String id, @Body CreateWorkoutRequest body);

    @GET("workouts/{id}/diary")
    Call<List<DiaryEntry>> diary(@Path("id") String workoutId);

    @POST("workouts/{id}/diary")
    Call<DiaryEntry> addDiaryEntry(@Path("id") String workoutId, @Body DiaryRequest body);

    @PUT("workout-diary/{id}")
    Call<DiaryEntry> updateDiaryEntry(@Path("id") String id, @Body DiaryRequest body);

    @DELETE("workout-diary/{id}")
    Call<ApiMessage> deleteDiaryEntry(@Path("id") String id);

    @DELETE("workouts/{id}")
    Call<ApiMessage> deleteWorkout(@Path("id") String id);

    @GET("workouts/{id}/checks")
    Call<List<ExerciseCheck>> checks(@Path("id") String workoutId,
                                     @Query("year") int year,
                                     @Query("week") int week);

    @POST("workouts/{id}/checks")
    Call<ExerciseCheck> toggleCheck(@Path("id") String workoutId, @Body CheckRequest body);

    @POST("workout-shares")
    Call<WorkoutShare> shareWorkout(@Body ShareRequest body);

    @GET("progress")
    Call<List<WorkoutProgress>> allProgress();

    @POST("workouts/{id}/progress/calculate")
    Call<WorkoutProgress> calculateProgress(@Path("id") String workoutId,
                                            @Query("year") int year,
                                            @Query("week") int week);
}
