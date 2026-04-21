const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendNotificationToUser = onDocumentCreated(
    "notifications/{notificationId}",
    async (event) => {
      try {
        const snapshot = event.data;

        if (!snapshot) {
          console.log("No snapshot data");
          return;
        }

        const notification = snapshot.data();

        if (!notification) {
          console.log("No notification data");
          return;
        }

        const userId = notification.userId;

        if (!userId) {
          console.log("No userId in notification");
          return;
        }

        const db = admin.firestore();

        let userDoc = await db.collection("users").doc(userId).get();

        if (!userDoc.exists) {
          userDoc = await db.collection("trainers").doc(userId).get();
        }

        if (!userDoc.exists) {
          console.log("User not found:", userId);
          return;
        }

        const user = userDoc.data();
        const token = user.fcmToken;

        if (!token) {
          console.log("No FCM token for user:", userId);
          return;
        }

        const payload = {
          token: token,
          data: {
            title: notification.title || "FitLink",
            message: notification.message || "New notification",
            type: notification.type || "GENERAL",
            userId: userId,
          },
        };

        await admin.messaging().send(payload);

        console.log("Notification sent to:", userId);
      } catch (error) {
        console.error("Notification error:", error);
      }
    },
);
