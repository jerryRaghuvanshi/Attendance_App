const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

exports.setDirectorClaim = functions.https.onCall(async (data, context) => {
  console.log('setDirectorClaim called with data:', data);
  console.log('Context auth:', context.auth);
  const { uid, role } = data;
  if (!uid || !role) {
    throw new functions.https.HttpsError('invalid-argument', 'Missing uid or role');
  }

  try {

    if (role === 'director') {

      const directorDoc = await admin.firestore().collection('directors').doc('director').get();

      if (directorDoc.exists) {
        throw new functions.https.HttpsError('already-exists', 'Director already exists');
      }
    } else {

      if (!context.auth) {
        throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
      }
    }

    console.log('Setting custom claims for uid:', uid, 'role:', role);


    await admin.auth().setCustomUserClaims(uid, {
      role: role,
      approved: role === 'teacher' ? false : true
    });

    console.log('Custom claims set successfully');


    if (role === 'director') {
      await admin.firestore().collection('directors').doc('director').set({
        uid: uid,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      console.log('Director document created in Firestore');
    }

    return { success: true, message: `Custom claims set for ${role}` };
  } catch (error) {
    console.error('Error in setDirectorClaim:', error);


    if (error.code) {
      throw error;
    } else {
      throw new functions.https.HttpsError('internal', `Error setting custom claims: ${error.message}`);
    }
  }
});


exports.approveTeacher = functions.https.onCall(async (data, context) => {

  if (!context.auth || context.auth.token.role !== 'director') {
    throw new functions.https.HttpsError('permission-denied', 'Only directors can approve teachers');
  }

  const { uid } = data;

  try {

    await admin.auth().setCustomUserClaims(uid, {
      role: 'teacher',
      approved: true
    });


    await admin.firestore().collection('users').doc(uid).update({
      approved: true,
      approvedAt: admin.firestore.FieldValue.serverTimestamp()
    });

    return { success: true, message: 'Teacher approved successfully' };
  } catch (error) {
    console.error('Error approving teacher:', error);
    throw new functions.https.HttpsError('internal', 'Error approving teacher');
  }
});


exports.getUserRole = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError('unauthenticated', 'User must be authenticated');
  }

  const userRecord = await admin.auth().getUser(context.auth.uid);
  return {
    uid: context.auth.uid,
    customClaims: userRecord.customClaims || {}
  };
});