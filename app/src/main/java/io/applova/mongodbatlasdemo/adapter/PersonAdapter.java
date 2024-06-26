package io.applova.mongodbatlasdemo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import io.applova.mongodbatlasdemo.R;
import io.applova.mongodbatlasdemo.async.TaskCallback;
import io.applova.mongodbatlasdemo.domain.Person;
import io.realm.Realm;

public class PersonAdapter extends RecyclerView.Adapter<PersonAdapter.PersonViewHolder> {

    private List<Person> personList;
    private Realm realm;
    private TaskCallback onItemDeleteCallback;

    public PersonAdapter(List<Person> personList, Realm realm, TaskCallback onItemDeleteCallback) {
        this.personList = personList;
        this.realm = realm;
        this.onItemDeleteCallback = onItemDeleteCallback;
    }

    @NonNull
    @Override
    public PersonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_person, parent, false);
        return new PersonViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull PersonViewHolder holder, int position) {
        Person person = personList.get(position);
        holder.nameTextView.setText(person.getName());
        holder.ageTextView.setText(String.valueOf(person.getAge()));

        holder.deleteButton.setOnClickListener(v -> onItemDeleteCallback.onTaskDone(person.get_id()));
    }

    @Override
    public int getItemCount() {
        return personList.size();
    }

    public void updateData(List<Person> newPersonList) {
        personList = newPersonList;
        notifyDataSetChanged();
    }

    static class PersonViewHolder extends RecyclerView.ViewHolder {

        TextView nameTextView;
        TextView ageTextView;
        Button deleteButton;

        PersonViewHolder(View view) {
            super(view);
            nameTextView = view.findViewById(R.id.nameTextView);
            ageTextView = view.findViewById(R.id.ageTextView);
            deleteButton = view.findViewById(R.id.deleteButton);
        }
    }
}

