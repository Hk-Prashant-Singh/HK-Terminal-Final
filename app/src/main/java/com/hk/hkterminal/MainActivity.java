<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:orientation="vertical">

    <!-- ===== TOP HACKER HEADER ===== -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:background="#0A0A0A"
        android:gravity="center_vertical"
        android:padding="10dp"
        android:elevation="8dp">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="🛡 PS HACKER"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold"
            android:fontFamily="monospace"/>

    </LinearLayout>

    <!-- ===== STATUS HEADER ===== -->
    <TextView
        android:id="@+id/statusHeader"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="#00FF00"
        android:textSize="12sp"
        android:fontFamily="monospace"
        android:text="[ SYSTEM ONLINE ]"
        android:gravity="end"
        android:padding="8dp"
        android:background="#111111"/>

    <!-- ===== TERMINAL OUTPUT AREA ===== -->
    <ScrollView
        android:id="@+id/scrollView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="8dp">

        <TextView
            android:id="@+id/outputView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#00FF00"
            android:textSize="15sp"
            android:fontFamily="monospace"
            android:lineSpacingExtra="4dp"/>
    </ScrollView>

    <!-- ===== COMMAND BAR ===== -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:orientation="horizontal"
        android:background="#0F0F0F"
        android:padding="8dp"
        android:gravity="center_vertical">

        <EditText
            android:id="@+id/inputCommand"
            android:layout_width="0dp"
            android:layout_height="match_parent"
            android:layout_weight="1"
            android:hint="root@pshacker:~$"
            android:textColorHint="#006600"
            android:textColor="#00FF00"
            android:background="#111111"
            android:padding="10dp"
            android:fontFamily="monospace"/>

        <Button
            android:id="@+id/runButton"
            android:layout_width="90dp"
            android:layout_height="match_parent"
            android:text="RUN"
            android:textStyle="bold"
            android:backgroundTint="#111111"
            android:textColor="#00FF00"
            android:fontFamily="monospace"/>
    </LinearLayout>

</LinearLayout>
