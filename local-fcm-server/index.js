const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');

// Initialize Express app
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin SDK with service account
const serviceAccount = require('../functions/medical-adherence-22fd2-firebase-adminsdk-fbsvc-9c82456821.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: 'medical-adherence-22fd2'
});

console.log('✅ Firebase Admin SDK initialized successfully');

/**
 * POST /sendNotification
 * 
 * Send FCM push notification to a device
 * 
 * Request Body:
 * {
 *   "token": "FCM_DEVICE_TOKEN",
 *   "title": "Notification Title",
 *   "body": "Notification Body"
 * }
 * 
 * Response:
 * {
 *   "success": true,
 *   "messageId": "projects/medical-adherence-22fd2/messages/..."
 * }
 */
app.post('/sendNotification', async (req, res) => {
  try {
    const { token, title, body } = req.body;

    // Validate required fields
    if (!token || !title || !body) {
      return res.status(400).json({
        success: false,
        error: 'Missing required fields: token, title, and body are required.'
      });
    }

    console.log(`📤 Sending notification to token: ${token.substring(0, 20)}...`);

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
    
    console.log('✅ Notification sent successfully:', response);

    return res.status(200).json({
      success: true,
      messageId: response
    });

  } catch (error) {
    console.error('❌ Error sending notification:', error);

    return res.status(500).json({
      success: false,
      error: error.message || 'Failed to send notification'
    });
  }
});

/**
 * GET /health
 * Health check endpoint
 */
app.get('/health', (req, res) => {
  res.json({
    status: 'ok',
    service: 'Local FCM Server',
    timestamp: new Date().toISOString()
  });
});

/**
 * GET /
 * Root endpoint with API info
 */
app.get('/', (req, res) => {
  res.json({
    name: 'Local FCM Notification Server',
    version: '1.0.0',
    endpoints: {
      'POST /sendNotification': 'Send FCM push notification',
      'GET /health': 'Health check'
    },
    usage: {
      method: 'POST',
      url: `http://localhost:${PORT}/sendNotification`,
      body: {
        token: 'FCM_DEVICE_TOKEN',
        title: 'Notification Title',
        body: 'Notification Body'
      }
    }
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`\n🚀 Local FCM Server running on http://localhost:${PORT}`);
  console.log(`📋 API Documentation: http://localhost:${PORT}`);
  console.log(`💚 Health Check: http://localhost:${PORT}/health`);
  console.log(`\n📤 Send notifications to: http://localhost:${PORT}/sendNotification\n`);
});

