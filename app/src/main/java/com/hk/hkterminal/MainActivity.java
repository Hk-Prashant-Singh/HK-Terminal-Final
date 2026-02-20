<LinearLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000"
    android:orientation="vertical">

    <RelativeLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="#0A0A0A"
        android:padding="10dp">

        <ImageView
            android:id="@+id/shieldLogo"
            android:layout_width="35dp"
            android:layout_height="35dp"
            android:src="@android:drawable/ic_lock_idle_lock" 
            android:layout_centerVertical="true"
            android:tint="#FF0000"/>

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_toEndOf="@id/shieldLogo"
            android:layout_centerVertical="true"
            android:layout_marginStart="10dp"
            android:text="PS HACKER"
            android:textColor="#FFFFFF"
            android:textSize="20sp"
            android:textStyle="bold"
            android:fontFamily="monospace"/>

        <ImageView
            android:layout_width="24dp"
            android:layout_height="24dp"
            android:layout_alignParentEnd="true"
            android:layout_centerVertical="true"
            android:src="@android:drawable/ic_menu_more"
            android:tint="#FFFFFF"/>
    </RelativeLayout>

    <TextView
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Ultimate: Command Bar"
        android:textColor="#AAAAAA"
        android:textSize="14sp"
        android:paddingStart="15dp"
        android:paddingTop="5dp"
        android:fontFamily="monospace"/>

    <ScrollView
        android:id="@+id/scrollView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:padding="12dp"
        android:fillViewport="true">

        <TextView
            android:id="@+id/outputView"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:textColor="#00FF00"
            android:textSize="14sp"
            android:fontFamily="monospace"
            android:lineSpacingMultiplier="1.2"/>
    </ScrollView>

    <TableLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="#111111"
        android:stretchColumns="*">

        <TableRow>
            <Button style="?android:attr/borderlessButtonStyle" android:text="ESC" android:textColor="#FFF" android:textSize="12sp" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="/" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="-" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="HOME" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="↑" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="END" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="PGUP" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
        </TableRow>

        <TableRow>
            <Button style="?android:attr/borderlessButtonStyle" android:text="↹" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="CTRL" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="ALT" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="←" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="↓" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="→" android:textColor="#FFF" android:fontFamily="monospace"/>
            <Button style="?android:attr/borderlessButtonStyle" android:text="PGDN" android:textColor="#FFF" android:textSize="10sp" android:fontFamily="monospace"/>
        </TableRow>
    </TableLayout>

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:background="#0A0A0A"
        android:padding="5dp">

        <EditText
            android:id="@+id/inputCommand"
            android:layout_width="0dp"
            android:layout_height="50dp"
            android:layout_weight="1"
            android:hint="Enter command"
            android:textColorHint="#444444"
            android:textColor="#00FF00"
            android:background="@null"
            android:paddingStart="10dp"
            android:fontFamily="monospace"
            android:inputType="textNoSuggestions"/>

        <Button
            android:id="@+id/runButton"
            android:layout_width="80dp"
            android:layout_height="40dp"
            android:layout_gravity="center_vertical"
            android:text="RUN"
            android:backgroundTint="#333333"
            android:textColor="#FFFFFF"
            android:fontFamily="monospace"/>
    </LinearLayout>

</LinearLayout>
