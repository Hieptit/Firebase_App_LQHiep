package com.example.firebase;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.firebase.models.Movie;
import com.example.firebase.models.Showtime;
import com.example.firebase.models.Ticket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MovieDetailActivity extends AppCompatActivity {

    private ImageView ivDetailPoster;
    private TextView tvDetailTitle, tvDetailGenre, tvDetailRating, tvDetailDescription;
    private Button btnConfirmBooking;
    private ImageButton btnBack;
    private Spinner spShowtimes;
    private Movie movie;
    private DatabaseReference dbRef;
    private DatabaseReference showtimeRef;
    private FirebaseAuth mAuth;
    private List<Showtime> showtimeList;
    private ArrayAdapter<Showtime> showtimeAdapter;

    private static final int NOTIFICATION_PERMISSION_CODE = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        checkNotificationPermission();
        createNotificationChannel();

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://movieticketapp-73a07-default-rtdb.asia-southeast1.firebasedatabase.app");
        dbRef = database.getReference("tickets");
        showtimeRef = database.getReference("showtimes");
        mAuth = FirebaseAuth.getInstance();

        ivDetailPoster = findViewById(R.id.ivDetailPoster);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);
        tvDetailGenre = findViewById(R.id.tvDetailGenre);
        tvDetailRating = findViewById(R.id.tvDetailRating);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);
        spShowtimes = findViewById(R.id.spShowtimes);
        btnBack = findViewById(R.id.btnBack);

        movie = (Movie) getIntent().getSerializableExtra("movie");

        if (movie != null) {
            tvDetailTitle.setText(movie.getTitle());
            tvDetailGenre.setText(movie.getGenre());
            tvDetailRating.setText("⭐ " + movie.getRating());
            tvDetailDescription.setText(movie.getDescription());

            Glide.with(this)
                    .load(movie.getPosterUrl())
                    .placeholder(android.R.drawable.ic_menu_report_image)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(ivDetailPoster);
            
            fetchShowtimes();
        }

        btnConfirmBooking.setOnClickListener(v -> bookTicket());
        btnBack.setOnClickListener(v -> finish());
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    private void fetchShowtimes() {
        showtimeList = new ArrayList<>();
        showtimeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, showtimeList);
        showtimeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spShowtimes.setAdapter(showtimeAdapter);

        showtimeRef.orderByChild("movieId").equalTo(movie.getId())
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        showtimeList.clear();
                        if (snapshot.exists()) {
                            for (DataSnapshot data : snapshot.getChildren()) {
                                Showtime st = data.getValue(Showtime.class);
                                if (st != null) {
                                    st.setId(data.getKey());
                                    showtimeList.add(st);
                                }
                            }
                        }
                        showtimeAdapter.notifyDataSetChanged();
                        
                        if (showtimeList.isEmpty()) {
                             Showtime empty = new Showtime();
                             empty.setTheaterName("Không có giờ chiếu");
                             empty.setTime("");
                             showtimeList.add(empty);
                             showtimeAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void bookTicket() {
        if (mAuth.getCurrentUser() == null) return;
        
        if (showtimeList.isEmpty() || (showtimeList.size() == 1 && showtimeList.get(0).getTime().isEmpty())) {
            Toast.makeText(this, "Vui lòng chọn giờ chiếu hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        Showtime selectedShowtime = (Showtime) spShowtimes.getSelectedItem();
        String userId = mAuth.getCurrentUser().getUid();
        
        Ticket ticket = new Ticket(
                null,
                userId,
                selectedShowtime.getId(),
                "Seat_Standard",
                new Date(),
                150000.0
        );

        dbRef.push().setValue(ticket)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(MovieDetailActivity.this, "Đặt vé thành công!", Toast.LENGTH_SHORT).show();
                    
                    // 1. Hiện thông báo thành công NGAY LẬP TỨC
                    sendSuccessNotification(selectedShowtime);
                    
                    // 2. Lên lịch thông báo NHẮC NHỞ sau 10 giây
                    scheduleReminderNotification(selectedShowtime);
                    
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MovieDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void sendSuccessNotification(Showtime showtime) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "movie_reminder")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Đặt vé thành công!")
                .setContentText("Bạn đã đặt vé phim " + movie.getTitle() + " lúc " + showtime.getTime())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(1, builder.build()); // ID 1 cho thành công
        }
    }

    private void scheduleReminderNotification(Showtime showtime) {
        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("movieTitle", movie.getTitle());
        intent.putExtra("showtime", showtime.getTime());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = System.currentTimeMillis() + 10000; // 10 giây sau

        if (alarmManager != null) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Movie Reminder";
            String description = "Channel for movie reminders";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("movie_reminder", name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
