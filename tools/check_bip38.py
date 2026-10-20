"""Independent decryption of PUBLIC compressed BIP38 fixtures, including EC-multiply."""
import hashlib
import pathlib
import subprocess
import unicodedata
from cryptography.hazmat.primitives.ciphers import Cipher, algorithms, modes
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.primitives.serialization import Encoding, PublicFormat

ROOT=pathlib.Path(__file__).resolve().parents[1]
ALPHABET='123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
ORDER=0xfffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141
OPENSSL_VERSION=subprocess.check_output(['openssl','version'],text=True).strip()

def ripemd160(data):
    # Hosted Python may omit RIPEMD-160. Match check_addresses.py's independently
    # installed system OpenSSL path, without changing application crypto policy.
    providers=['-provider','legacy'] if OPENSSL_VERSION.startswith('OpenSSL 3.') else []
    digest=subprocess.check_output(['openssl','dgst',*providers,'-ripemd160','-binary'],input=data)
    assert len(digest)==20
    return digest

def sha(data):return hashlib.sha256(hashlib.sha256(data).digest()).digest()
def decode(text):
    value=0
    for c in text:value=value*58+ALPHABET.index(c)
    raw=bytes(len(text)-len(text.lstrip('1')))+value.to_bytes((value.bit_length()+7)//8,'big')
    assert sha(raw[:-4])[:4]==raw[-4:]
    return raw[:-4]
def encode(data):
    raw=data+sha(data)[:4];value=int.from_bytes(raw,'big');text=''
    while value:value,i=divmod(value,58);text=ALPHABET[i]+text
    return '1'*(len(raw)-len(raw.lstrip(b'\0')))+text

def pub(value):return ec.derive_private_key(value%ORDER,ec.SECP256K1()).public_key().public_bytes(Encoding.X962,PublicFormat.CompressedPoint)
def xor(a,b):return bytes(x^y for x,y in zip(a,b))
def derive(pw,salt,n=16384,r=8,p=8,length=64):return hashlib.scrypt(pw,salt=salt,n=n,r=r,p=p,dklen=length,maxmem=128*1024*1024)
def aes(key,block):
    operation=Cipher(algorithms.AES(key),modes.ECB()).decryptor()
    return operation.update(block)+operation.finalize()
def decrypt(text,password):
    data=decode(text);assert len(data)==39 and data[0]==1
    kind,flag=data[1:3];assert flag in ({0xe0} if kind==0x42 else {0x20,0x24})
    address_hash=data[3:7];content=data[7:];pw=unicodedata.normalize('NFC',password).encode()
    if kind==0x42:
        derived=derive(pw,address_hash);raw=xor(aes(derived[32:],content),derived[:32])
    else:
        assert kind==0x43
        owner=content[:8];factor=derive(pw,owner[:4] if flag&4 else owner,length=32)
        if flag&4:factor=sha(factor+owner)
        p=int.from_bytes(factor,'big');derived=derive(pub(p),address_hash+owner,n=1024,r=1,p=1)
        second=xor(aes(derived[32:],content[16:]),derived[16:32])
        first=xor(aes(derived[32:],content[8:16]+second[:8]),derived[:16])
        raw=((p*int.from_bytes(sha(first+second[8:]),'big'))%ORDER).to_bytes(32,'big')
    scalar=int.from_bytes(raw,'big');assert 0<scalar<ORDER
    address=encode(b'\0'+ripemd160(hashlib.sha256(pub(scalar)).digest()))
    assert sha(address.encode())[:4]==address_hash
    return raw,encode(b'\x80'+raw+b'\x01')

if __name__=='__main__':
    if not __debug__:raise RuntimeError('Assertions must remain enabled')
    assert ripemd160(b'').hex()=='9c1185a5c5e9fc54612808977ee8f548b2258d31'
    count=0
    for line in (ROOT/'core/src/test/resources/bip38-public-vectors.tsv').read_text().splitlines():
        if line.startswith('#') or not line:continue
        name,cipher,password,expected,wif,result=line.split('\t')
        if result=='reject':
            try:decrypt(cipher,password)
            except AssertionError:pass
            else:raise AssertionError('Invalid fixture accepted: '+name)
        else:
            raw,actual=decrypt(cipher,password);assert raw.hex()==expected and actual==wif
        count+=1
    print(f'Independent Python BIP38 verification PASS: {count} public fixtures; compressed boundaries, EC modes, NFC and rejection checks.')
