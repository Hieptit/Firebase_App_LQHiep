package com.example.firebase;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.firebase.models.Movie;
import com.example.firebase.models.Showtime;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvMovies;
    private MovieAdapter adapter;
    private List<Movie> movieList;
    private DatabaseReference dbRef;
    private DatabaseReference showtimeRef;
    private FirebaseAuth mAuth;
    private FloatingActionButton btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://movieticketapp-73a07-default-rtdb.asia-southeast1.firebasedatabase.app");
        dbRef = database.getReference("movies");
        showtimeRef = database.getReference("showtimes");
        mAuth = FirebaseAuth.getInstance();

        rvMovies = findViewById(R.id.rvMovies);
        btnLogout = findViewById(R.id.btnLogout);

        movieList = new ArrayList<>();
        adapter = new MovieAdapter(movieList, movie -> {
            Intent intent = new Intent(MainActivity.this, MovieDetailActivity.class);
            intent.putExtra("movie", movie);
            startActivity(intent);
        });

        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        addSampleData();
        fetchMovies();
    }

    private void fetchMovies() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movieList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Movie movie = data.getValue(Movie.class);
                    if (movie != null) {
                        movie.setId(data.getKey());
                        movieList.add(movie);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.w("MainActivity", "Lỗi đọc dữ liệu: ", error.toException());
            }
        });
    }

    private void addSampleData() {
        dbRef.removeValue();
        showtimeRef.removeValue();

        // Avengers
        String id1 = dbRef.push().getKey();
        Movie m1 = new Movie(id1, "Avengers: Endgame", 
            "Biệt đội siêu anh hùng tập hợp để chống lại Thanos.", 
            "https://m.media-amazon.com/images/M/MV5BMTc5MDE2ODcwNV5BMl5BanBnXkFtZTgwMzI2NzQ2NzM@._V1_FMjpg_UX1000_.jpg", 
            "Action/Adventure", 8.4, 181);
        dbRef.child(id1).setValue(m1);

        // Inception
        String id2 = dbRef.push().getKey();
        Movie m2 = new Movie(id2, "Inception", 
            "Kẻ trộm giấc mơ với những kế hoạch điên rồ.", 
            "https://m.media-amazon.com/images/M/MV5BMjAxMzY3NjcxNF5BMl5BanBnXkFtZTcwNTI5OTM0Mw@@._V1_FMjpg_UX1000_.jpg", 
            "Sci-fi/Action", 8.8, 148);
        dbRef.child(id2).setValue(m2);

        // Spiderman
        String id3 = dbRef.push().getKey();
        Movie m3 = new Movie(id3, "Spiderman: No Way Home", 
            "Đa vũ trụ mở ra, Peter Parker đối mặt thử thách lớn nhất.", 
            "https://m.media-amazon.com/images/M/MV5BMGZlNTY1ZWUtYTMzNC00ZjUyLWE0MjYtOTk0ZDhcXGBXG_XkFtZTgwNzIyMDA4MDE@._V1_FMjpg_UX1000_.jpg", 
            "Action/Sci-fi", 8.2, 148);
        dbRef.child(id3).setValue(m3);

        addShowtime(id1, "CGV Vincom", "19:00", "2023-12-30");
        addShowtime(id1, "Lotte Cinema", "21:30", "2023-12-30");
        addShowtime(id2, "BHD Star", "18:00", "2023-12-31");
        addShowtime(id3, "Galaxy Cinema", "20:00", "2023-12-30");
    }

    private void addShowtime(String movieId, String theater, String time, String date) {
        String stId = showtimeRef.push().getKey();
        Showtime st = new Showtime(stId, movieId, theater, time, date);
        showtimeRef.child(stId).setValue(st);
    }
}
