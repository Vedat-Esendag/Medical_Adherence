const functions = require('firebase-functions');
const admin = require('firebase-admin');
const cors = require('cors')({ origin: true });

// Initialize Firebase Admin SDK with service account
const serviceAccount = require('./medical-adherence-22fd2-firebase-adminsdk-fbsvc-9c82456821.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'medical-adherence-22fd2'
});

/**
 * HTTPS Cloud Function: Send FCM Push Notification
 * 
 * Endpoint: https://us-central1-medical-adherence-22fd2.cloudfunctions.net/sendNotification
 * Method: POST
 * 
 * Request Body (JSON):
 * {
 *   "token": "FCM_DEVICE_TOKEN",
 *   "title": "Notification Title",
 *   "body": "Notification Body"
 * }
 * 
 * Response (JSON):
 * {
 *   "success": true,
 *   "messageId": "projects/medical-adherence-22fd2/messages/..."
 * }
 * OR
 * {
 *   "success": false,
 *   "error": "Error message"
 * }
 */
exports.sendNotification = functions.https.onRequest(async (req, res) => {
  // Enable CORS for cross-origin requests
  return cors(req, res, async () => {
    // Only allow POST requests
    if (req.method !== 'POST') {
      return res.status(405).json({
        success: false,
        error: 'Method not allowed. Use POST.'
      });
    }

    try {
      // Extract parameters from request body
      const { token, title, body } = req.body;

      // Validate required fields
      if (!token || !title || !body) {
        return res.status(400).json({
          success: false,
          error: 'Missing required fields: token, title, and body are required.'
        });
      }

      console.log(`Sending notification to token: ${token.substring(0, 20)}...`);

      // Construct FCM message
      const message = {
        notification: {
          title: title,
          body: body
        },
        token: token,
        android: {
          priority: 'high',
          notification: {
            sound: 'default',
            channelId: 'medication_reminders'
          }
        }
      };

      // Send notification via Firebase Admin SDK
      const response = await admin.messaging().send(message);
      
      console.log('Successfully sent notification:', response);

      return res.status(200).json({
        success: true,
        messageId: response
      });

    } catch (error) {
      console.error('Error sending notification:', error);

      return res.status(500).json({
        success: false,
        error: error.message || 'Failed to send notification'
      });
    }
  });
});

/**
 * OPTIONAL: Firestore Trigger Function (Keep if you need it)
 * Automatically sends notifications when documents are created in 'notificationRequests'
 */
exports.sendNotificationOnCreate = functions.firestore
  .document('notificationRequests/{requestId}')
  .onCreate(async (snap, context) => {
    const data = snap.data();

    if (data.sent) {
      console.log('Notification already sent, skipping');
      return null;
    }

    try {
      console.log(`Processing notification request for patient PIN: ${data.patientPin}`);

      const usersSnapshot = await admin.firestore()
        .collection('users')
        .where('pin', '==', data.patientPin)
        .where('role', '==', 'Patient')
        .limit(1)
        .get();

      if (usersSnapshot.empty) {
        console.error('Patient not found with PIN:', data.patientPin);
        await snap.ref.update({
          sent: false,
          error: 'Patient not found with this PIN'
        });
        return null;
      }

      const patientDoc = usersSnapshot.docs[0];
      const patientData = patientDoc.data();
      const fcmToken = patientData.fcmToken;

      if (!fcmToken) {
        console.error('Patient has no FCM token');
        await snap.ref.update({
          sent: false,
          error: 'Patient has no FCM token registered'
        });
        return null;
      }

      const message = {
        notification: {
          title: data.title || 'Medication Reminder',
          body: data.body || 'You have a message from your caregiver'
        },
        token: fcmToken
      };

      await admin.messaging().send(message);
      console.log('Notification sent successfully');

      await snap.ref.update({ 
        sent: true, 
        sentAt: admin.firestore.FieldValue.serverTimestamp() 
      });

      return null;
    } catch (error) {
      console.error('Error sending notification:', error);
      await snap.ref.update({
        sent: false,
        error: error.message,
        errorAt: admin.firestore.FieldValue.serverTimestamp()
      });
      return null;
    }
  });
