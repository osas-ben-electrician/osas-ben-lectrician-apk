package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.DeviceStatus
import com.example.data.model.EmergencyReport
import com.example.data.model.RegisteredDevice
import com.example.data.model.ReportStatus
import com.example.data.model.SupportedCountry
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user profile observation." }
        val docRef = db.collection("users").document(uid)
        docRef.snapshots()
            .map { snapshot ->
                if (snapshot.exists()) {
                    snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                } else {
                    null
                }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, docRef.path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun saveUserProfile(profile: UserProfile): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path

            val safeFirstName = profile.firstName.trim().take(80)
            val safeEmail = profile.email.trim().take(160)
            val safeCountry = SupportedCountry.fromWire(profile.country).wireName
            val safeRegion = profile.region.trim().take(100)
            val safeProvince = profile.province?.trim()?.take(100)?.takeIf { it.isNotEmpty() }
            val safeLocalArea = profile.localArea.trim().take(120)
            val safeLang = if (profile.language == "es") "es" else "en"

            require(safeFirstName.isNotEmpty()) { "First name required" }
            require(safeEmail.length >= 3) { "Valid email required" }
            require(safeRegion.isNotEmpty()) { "Region required" }
            require(safeLocalArea.isNotEmpty()) { "Local area required" }

            val existing = docRef.get().await()
            if (existing.exists()) {
                val updateMap = mutableMapOf<String, Any>(
                    "firstName" to safeFirstName,
                    "email" to safeEmail,
                    "country" to safeCountry,
                    "region" to safeRegion,
                    "localArea" to safeLocalArea,
                    "language" to safeLang,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeProvince != null) {
                    updateMap["province"] = safeProvince
                }
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "firstName" to safeFirstName,
                    "email" to safeEmail,
                    "country" to safeCountry,
                    "region" to safeRegion,
                    "province" to safeProvince,
                    "localArea" to safeLocalArea,
                    "language" to safeLang,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeCountryEmergencyReports(country: SupportedCountry): Flow<List<EmergencyReport>> = flow {
        val uid = requireUserId()
        val path = "emergency_reports"
        db.collection(path)
            .whereEqualTo("country", country.wireName)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    EmergencyReport::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun submitEmergencyReport(report: EmergencyReport): Result<String> {
        var docPath = "emergency_reports"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("emergency_reports").document()
            docPath = docRef.path

            val safeCountry = SupportedCountry.fromWire(report.country).wireName
            val safeRegion = report.region.trim().ifBlank { safeCountry }.take(100)
            val safeProvince = report.province?.trim()?.take(100)?.takeIf { it.isNotEmpty() }
            val safeLocalArea = report.localArea.trim().ifBlank { safeRegion }.take(120)
            val safeNote = report.note?.trim()?.take(300)?.takeIf { it.isNotEmpty() }

            val payload = mapOf(
                "userId" to uid,
                "emergencyType" to report.typeEnum().name,
                "status" to ReportStatus.ACTIVE.name,
                "country" to safeCountry,
                "region" to safeRegion,
                "province" to safeProvince,
                "localArea" to safeLocalArea,
                "latitude" to report.latitude.coerceIn(-90.0, 90.0),
                "longitude" to report.longitude.coerceIn(-180.0, 180.0),
                "reportedTime" to report.reportedTime.take(40),
                "reportedDate" to report.reportedDate.take(40),
                "testAlert" to report.testAlert,
                "discreetMode" to report.discreetMode,
                "note" to safeNote,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateEmergencyLocation(reportId: String, latitude: Double, longitude: Double): Result<Unit> {
        val docPath = "emergency_reports/$reportId"
        return try {
            requireUserId()
            require(reportId.isNotBlank()) { "Report ID required" }
            db.collection("emergency_reports").document(reportId).update(
                mapOf(
                    "latitude" to latitude.coerceIn(-90.0, 90.0),
                    "longitude" to longitude.coerceIn(-180.0, 180.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateEmergencyStatus(
        reportId: String,
        newStatus: ReportStatus,
        note: String?
    ): Result<Unit> {
        val docPath = "emergency_reports/$reportId"
        return try {
            requireUserId()
            require(reportId.isNotBlank()) { "Report ID required" }
            require(newStatus == ReportStatus.RESOLVED || newStatus == ReportStatus.CANCELLED_FALSE_ALARM) {
                "Invalid terminal status transition"
            }
            val payload = mapOf(
                "status" to newStatus.name,
                "note" to note?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("emergency_reports").document(reportId).update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserDevices(userId: String): Flow<List<RegisteredDevice>> = flow {
        val uid = requireUserId()
        check(uid == userId) { "Refusing cross-user device observation." }
        val path = "users/$uid/devices"
        db.collection("users").document(uid).collection("devices")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    RegisteredDevice::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == uid) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun upsertDevice(deviceId: String, device: RegisteredDevice): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/devices/$deviceId"
        return try {
            val uid = requireUserId()
            require(deviceId.isNotBlank()) { "Device ID required" }
            val docRef = db.collection("users").document(uid).collection("devices").document(deviceId)
            docPath = docRef.path

            val safeName = device.deviceName.trim().take(100)
            val safeModel = device.deviceModel.trim().take(100)
            val safeCountry = SupportedCountry.fromWire(device.country).wireName
            val safeStatus = DeviceStatus.fromWire(device.status).name
            val safeUpdateText = device.lastLocationUpdateText.trim().take(60)

            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "deviceName" to safeName,
                        "deviceModel" to safeModel,
                        "country" to safeCountry,
                        "latitude" to device.latitude.coerceIn(-90.0, 90.0),
                        "longitude" to device.longitude.coerceIn(-180.0, 180.0),
                        "status" to safeStatus,
                        "lastLocationUpdateText" to safeUpdateText,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "deviceName" to safeName,
                        "deviceModel" to safeModel,
                        "country" to safeCountry,
                        "latitude" to device.latitude.coerceIn(-90.0, 90.0),
                        "longitude" to device.longitude.coerceIn(-180.0, 180.0),
                        "status" to safeStatus,
                        "lastLocationUpdateText" to safeUpdateText,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteDevice(deviceId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/devices/$deviceId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("devices").document(deviceId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pingConnection(): Result<String> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in with Google"))
        val docPath = "users/$uid"
        return try {
            val snap = db.collection("users").document(uid).get().await()
            val dbId = db.app.options.projectId ?: "firebase"
            Result.success("Connected ($dbId, profileExists=${snap.exists()})")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docPath)
            Result.failure(e)
        }
    }
}
