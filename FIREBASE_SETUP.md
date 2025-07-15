# 🔥 Firebase Setup Guide

## 📋 **Prerequisites**

You need to set up your own Firebase project to use this app. Follow these steps:

## 🚀 **Setup Instructions**

### **Step 1: Create Firebase Project**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name (e.g., "my-unilocator")
4. Follow setup wizard

### **Step 2: Enable Required Services**

1. **Authentication**:
   - Go to Authentication > Sign-in method
   - Enable Email/Password authentication
   - Enable Google sign-in (optional)

2. **Firestore Database**:
   - Go to Firestore Database
   - Create database in production mode
   - Set up security rules (see below)

3. **Storage** (if using file uploads):
   - Go to Storage
   - Get started with default rules

### **Step 3: Add Android App**

1. In Firebase Console, click "Add app" > Android
2. Enter package name: `com.pramanshav.unilocator`
3. Download `google-services.json`
4. Place it in `app/` folder (replace the template)

### **Step 4: Configure Security Rules**

#### **Firestore Rules:**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users can only access their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Device codes - users can read/write their own codes
    match /user_device_codes/{codeId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // User devices - users can manage their own devices
    match /user_devices/{deviceId} {
      allow read, write: if request.auth != null && 
        request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && 
        request.auth.uid == request.resource.data.userId;
    }
    
    // Device connections - users can access devices they're connected to
    match /device_connections/{connectionId} {
      allow read, write: if request.auth != null && 
        (request.auth.uid == resource.data.ownerUserId || 
         request.auth.uid == resource.data.connectedUserId);
      allow create: if request.auth != null;
    }
  }
}
```

## 🔧 **Project Configuration**

### **Required Firebase Features:**
- ✅ Authentication (Email/Password)
- ✅ Firestore Database
- ✅ Cloud Functions (optional, for advanced features)
- ✅ Storage (if using file uploads)

### **Collections Structure:**
```
users/
  {userId}/
    - email: string
    - displayName: string
    - createdAt: timestamp

user_device_codes/
  {codeId}/
    - deviceCode: string
    - userId: string
    - isActive: boolean
    - expiresAt: timestamp
    - createdAt: timestamp

user_devices/
  {deviceId}/
    - deviceId: string
    - userId: string
    - deviceName: string
    - deviceModel: string
    - isActive: boolean
    - registeredAt: timestamp
    - lastSeenAt: timestamp

device_connections/
  {connectionId}/
    - ownerUserId: string
    - connectedUserId: string
    - deviceName: string
    - connectedAt: timestamp
    - isActive: boolean
```

## 🛡️ **Security Best Practices**

1. **Never commit `google-services.json`** to public repositories
2. Use **Firestore Security Rules** to protect data
3. Enable **App Check** for production apps
4. Use **Environment Variables** for sensitive configuration
5. Enable **Firebase Security Rules** for all services

## 🔍 **Testing Your Setup**

1. Build and run the app
2. Try creating an account
3. Test device registration
4. Verify data appears in Firebase Console

## ⚠️ **Important Notes**

- The `google-services.json.template` file contains placeholder values
- Replace it with your actual Firebase configuration
- Keep your Firebase project private if it contains sensitive data
- Consider using Firebase App Distribution for beta testing

## 🆘 **Troubleshooting**

### **Common Issues:**

1. **"Default FirebaseApp is not initialized"**
   - Ensure `google-services.json` is in the correct location
   - Check that Firebase plugin is applied in `build.gradle`

2. **Permission denied errors**
   - Verify Firestore security rules
   - Check user authentication status

3. **Build errors**
   - Ensure all Firebase dependencies are added
   - Sync project with Gradle files

---

**Need help?** Check the [Firebase Documentation](https://firebase.google.com/docs/android/setup) for more details.
