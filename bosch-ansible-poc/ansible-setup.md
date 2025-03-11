Create key in localhost:

```shell
ssh-keygen -t ed25519 -C "crd1si@BF4-C-002QP" -f ~/.ssh/id_ansible_ed25519
```

This creates:

* **Private key:** `~/.ssh/id_ansible_ed25519`
* **Public key:** `~/.ssh/id_ansible_ed25519.pub`

Add key to inventory . Ex:

```yaml
    server_pool_1:
      hosts:
        vm1:
        vm2:
      vars:
        equipment_pool: Pool_A
        ansible_user: username
        ansible_ssh_private_key_file: ~/.ssh/id_ansible_ed25519
```

add key to ssh agent
```shell
ssh-add ~/.ssh/id_ansible_ed25519
```

confirmm it is installed
```shell
ssh-add -l
```


Distribute the keys to hosts

```shell
ssh-copy-id -i ~/.ssh/id_ed25519.pub username@remote-host
```
like
![alt text](image/ansible-setup/2025-02-21_12h08_48.png)
which leads to the key being added to authorized keys, such as 
![alt text](image/ansible-setup/distributed_key.png)


Or with Vaullt :
![alt text](image/ansible-setup/2025-02-21_11h27_00.png)

then copy the ansiblle_password to the inventory under vars

ansible-playbook playbook.yml --ask-vault-pass

or via pipeline : run: echo "$ANSIBLE_VAULT_PASSWORD" | ansible-playbook playbook.yml --vault-password-file /dev/stdin




test connectoin : ansible all -i inventory.ini -m ping
