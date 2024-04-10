package io.applova.mongodbatlasdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import io.applova.mongodbatlasdemo.databinding.ActivityMainBinding;
import io.realm.Realm;
import io.realm.mongodb.App;
import io.realm.mongodb.AppConfiguration;
import io.realm.mongodb.Credentials;
import io.realm.mongodb.User;
import io.realm.mongodb.sync.MutableSubscriptionSet;
import io.realm.mongodb.sync.Subscription;
import io.realm.mongodb.sync.SyncConfiguration;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'mongodbatlasdemo' library on application startup.
    static {
        System.loadLibrary("mongodbatlasdemo");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Example of a call to a native method
        TextView tv = binding.sampleText;
        tv.setText(stringFromJNI());

        // instantiate a Realm App connection
        String appID = YOUR_APP_ID; // replace this with your App ID
        App app = new App(new AppConfiguration.Builder(appID)
                .build());
// authenticate a user
        Credentials credentials = Credentials.anonymous();
        app.loginAsync(credentials, it -> {
            if (it.isSuccess()) {
                User user = it.get();
                // add an initial subscription to the sync configuration
                SyncConfiguration config = new SyncConfiguration.Builder(app.currentUser())
                        .initialSubscriptions(new SyncConfiguration.InitialFlexibleSyncSubscriptions() {
                            @Override
                            public void configure(Realm realm, MutableSubscriptionSet subscriptions) {
                                subscriptions.add(Subscription.create("subscriptionName",
                                        realm.where(Frog.class)
                                                .equalTo("species", "spring peeper")));
                            }
                        })
                        .build();
                // instantiate a realm instance with the flexible sync configuration
                Realm.getInstanceAsync(config, new Realm.Callback() {
                    @Override
                    public void onSuccess(Realm realm) {
                        Log.v("EXAMPLE", "Successfully opened a realm.");
                    }
                });
            } else {
                Log.e("EXAMPLE", "Failed to log in: " + it.getError().getErrorMessage());
            }
        });
    }

    /**
     * A native method that is implemented by the 'mongodbatlasdemo' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}